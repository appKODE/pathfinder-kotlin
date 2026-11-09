package ru.kode.pathfinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

@OptIn(ExperimentalAtomicApi::class)
class PathFinder private constructor(val store: Store, initialEnvironment: Environment) {

  companion object {
    /**
     * Saves [configuration] to the [store] and creates a [PathFinder] which is ready to build urls
     * as soon as this function returns.
     */
    suspend fun create(store: Store, configuration: Configuration): PathFinder {
      store.saveConfiguration(configuration)
      return PathFinder(store, store.readActiveEnvironment()).apply {
        fallbackConfigurations.value = store.fallbackConfigurations().first()
        lastSuccessfulFallbackUrls.value = store.lastSuccessfulFallbackUrls().first()
      }
    }

    private suspend fun Store.readActiveEnvironment(): Environment {
      val id = readActiveEnvironmentId() ?: error("internal error: no active environment id")
      return readEnvironmentById(id) ?: error("internal error: no active environment by id = ${id.value}")
    }
  }

  private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mutationMutex = Mutex()
  private val refreshMutex = Mutex()

  private val _currentEnvironment = MutableStateFlow(initialEnvironment)

  /**
   * The active environment. Reflects the latest switch and the latest change of its properties (e.g. `baseUrl`).
   */
  val currentEnvironment: Environment get() = _currentEnvironment.value

  /**
   * Emits [currentEnvironment] and each of its later changes, the same ones [Listener] is notified about.
   */
  val currentEnvironmentFlow: StateFlow<Environment> = _currentEnvironment.asStateFlow()

  private val fallbackConfigurations = MutableStateFlow(emptyList<EnvironmentFallbackConfiguration>())
  private val lastSuccessfulFallbackUrls = MutableStateFlow(emptyMap<String, String>())
  private val fallbackRegionCode = MutableStateFlow<String?>(null)
  private val fallbackMutex = Mutex()

  // copy-on-write: listeners are notified without a lock
  private val listeners = AtomicReference(emptyList<Listener>())

  init {
    // the store can also be changed directly (e.g. by the debug panel), so follow it
    merge(store.activeEnvironmentId(), store.environments())
      .onEach { refresh() }
      .launchIn(storeScope)
  }

  /**
   * Builds url for [id] using the active environment. See [PathResolver.buildUrl].
   */
  suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String> = emptyMap(),
    queryParameters: Map<String, String> = emptyMap(),
  ): String = buildUrl(currentEnvironment.id, id, pathVariables, queryParameters)

  /**
   * Builds url for [id] using environment [environmentId] instead of the active one.
   * See [PathResolver.buildUrl].
   */
  suspend fun buildUrl(
    environmentId: EnvironmentId,
    id: UrlSpecId,
    pathVariables: Map<String, String> = emptyMap(),
    queryParameters: Map<String, String> = emptyMap(),
  ): String = DefaultResolver(store, environmentId).buildUrl(id, pathVariables, queryParameters)

  interface Listener {
    /**
     * Will be called on switch or change of active environment.
     *
     * WARNING: This method will be called on the background thread
     */
    fun onEnvironmentSwitch(environmentId: EnvironmentId)
  }

  /**
   * Makes [environmentId] active. [currentEnvironment] and [buildUrl] reflect it when this function returns.
   */
  suspend fun switchEnvironment(environmentId: EnvironmentId) = mutate {
    checkNotNull(store.readEnvironmentById(environmentId)) { "no environment with id = ${environmentId.value}" }
    store.changeActiveEnvironment(environmentId)
  }

  /**
   * Replaces properties of the environment with the same id, or adds a new environment.
   */
  suspend fun replaceEnvironment(environment: Environment) = mutate {
    store.replaceEnvironment(environment)
  }

  /**
   * Replaces environment [environmentId] with the result of [updater].
   */
  suspend fun updateEnvironment(environmentId: EnvironmentId, updater: (Environment) -> Environment) = mutate {
    val environment = store.readEnvironmentById(environmentId)
      ?: error("no environment with id = ${environmentId.value}")
    store.replaceEnvironment(checkedUpdate(environment, updater))
  }

  suspend fun updateEachEnvironment(updater: (Environment) -> Environment) = mutate {
    store.readEnvironments().forEach { store.replaceEnvironment(checkedUpdate(it, updater)) }
  }

  /**
   * Saves a new [configuration] at runtime. If it differs from the stored one, all stored environments,
   * urls and their overrides are replaced. The active environment is kept if the new configuration has it.
   */
  suspend fun saveConfiguration(configuration: Configuration) = mutate {
    store.saveConfiguration(configuration)
  }

  fun addListener(listener: Listener) {
    listeners.update { it + listener }
  }

  fun removeListener(listener: Listener) {
    listeners.update { it - listener }
  }

  private suspend fun mutate(block: suspend () -> Unit) {
    mutationMutex.withLock { block() }
    refresh()
  }

  // Always reads the latest store state, so a late flow emission can never roll back a newer state
  private suspend fun refresh() = refreshMutex.withLock {
    val environment = store.readActiveEnvironment()
    if (environment != _currentEnvironment.value) {
      _currentEnvironment.value = environment
      listeners.load().forEach { it.onEnvironmentSwitch(environment.id) }
    }
  }

  /**
   * Replaces all fallback configurations and stores them.
   */
  suspend fun updateFallbackEnvironments(configurations: List<EnvironmentFallbackConfiguration>) {
    fallbackMutex.withLock {
      store.saveFallbackConfigurations(configurations)
      fallbackConfigurations.value = configurations
    }
  }

  /**
   * Selects the fallback configuration of region [code]. Without a configuration for it (or with a null [code])
   * the one of [DEFAULT_ENVIRONMENT_PARAMETERS_KEY] is used.
   */
  fun updateApiFallbackRegionCode(code: String?) {
    fallbackRegionCode.value = code
  }

  /**
   * Builds a plan for one request: [primaryBaseUrl] (usually `currentEnvironment.baseUrl`) and the fallback
   * base urls of the current region in their configured order, except that the one which last succeeded
   * (see [markFallbackSuccess]) goes first. With [allowFallbacks] = false the plan has [primaryBaseUrl] only.
   */
  fun readFallbackPlan(primaryBaseUrl: String, allowFallbacks: Boolean = true): FallbackRequestPlan {
    val regionCode = fallbackRegionCode.value
    val configurations = fallbackConfigurations.value
    val configuration = configurations.find { it.regionCode == regionCode }
      ?: configurations.find { it.regionCode == DEFAULT_ENVIRONMENT_PARAMETERS_KEY }
    val planRegionCode = configuration?.regionCode ?: regionCode ?: DEFAULT_ENVIRONMENT_PARAMETERS_KEY
    val candidates = if (allowFallbacks) {
      val urls = listOf(primaryBaseUrl) + configuration?.baseUrls.orEmpty()
      val lastSuccessful = lastSuccessfulFallbackUrls.value[planRegionCode]?.takeIf { it in urls }
      if (lastSuccessful != null) listOf(lastSuccessful) + urls.filter { it != lastSuccessful } else urls
    } else {
      listOf(primaryBaseUrl)
    }
    return FallbackRequestPlan(
      initialBaseUrl = candidates.first(),
      retryBaseUrls = candidates.drop(1),
      requestTimeout = configuration?.requestTimeout,
      regionCode = planRegionCode,
    )
  }

  /**
   * Remembers [baseUrl] (primary or a fallback of [FallbackRequestPlan]) as the one which served a request in
   * [regionCode] (see [FallbackRequestPlan.regionCode]), so the next [readFallbackPlan] tries it first.
   * Takes effect immediately, the store is updated in background.
   */
  fun markFallbackSuccess(regionCode: String, baseUrl: String) {
    if (lastSuccessfulFallbackUrls.value[regionCode] == baseUrl) return
    lastSuccessfulFallbackUrls.update { it + (regionCode to baseUrl) }
    storeScope.launch {
      // writes the latest value, so that concurrent calls can't leave an older one in the store
      fallbackMutex.withLock {
        lastSuccessfulFallbackUrls.value[regionCode]?.let { store.saveLastSuccessfulFallbackUrl(regionCode, it) }
      }
    }
  }

  private fun checkedUpdate(environment: Environment, updater: (Environment) -> Environment): Environment {
    val new = updater(environment)
    check(new.id == environment.id) {
      "environment id changed during an update operation. This is not allowed. " +
        "Old id=${environment.id}, new id=${new.id}"
    }
    return new
  }
}
