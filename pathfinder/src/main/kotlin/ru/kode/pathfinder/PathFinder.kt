package ru.kode.pathfinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList

class PathFinder private constructor(val store: Store, initialEnvironment: Environment) {

  companion object {
    /**
     * Saves [configuration] to the [store] and creates a [PathFinder] which is ready to build urls
     * as soon as this function returns.
     */
    suspend fun create(store: Store, configuration: Configuration): PathFinder {
      store.saveConfiguration(configuration)
      return PathFinder(store, store.readActiveEnvironment())
    }

    private suspend fun Store.readActiveEnvironment(): Environment {
      val id = readActiveEnvironmentId() ?: error("internal error: no active environment id")
      return readEnvironmentById(id) ?: error("internal error: no active environment by id = ${id.value}")
    }
  }

  private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mutationMutex = Mutex()
  private val refreshMutex = Mutex()

  @Volatile
  private var _currentEnvironment: Environment = initialEnvironment

  /**
   * The active environment. Reflects the latest switch and the latest change of its properties (e.g. `baseUrl`).
   */
  val currentEnvironment: Environment get() = _currentEnvironment

  private val listeners: MutableList<Listener> = CopyOnWriteArrayList()

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
  ): String = buildUrl(_currentEnvironment.id, id, pathVariables, queryParameters)

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
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  private suspend fun mutate(block: suspend () -> Unit) {
    mutationMutex.withLock { block() }
    refresh()
  }

  // Always reads the latest store state, so a late flow emission can never roll back a newer state
  private suspend fun refresh() = refreshMutex.withLock {
    val environment = store.readActiveEnvironment()
    if (environment != _currentEnvironment) {
      _currentEnvironment = environment
      listeners.forEach { it.onEnvironmentSwitch(environment.id) }
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
