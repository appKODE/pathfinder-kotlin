package ru.kode.pathfinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PathFinder private constructor(
  val store: Store,
) {

  companion object {
    suspend fun create(store: Store, configuration: Configuration): PathFinder {
      withContext(Dispatchers.IO) {
        store.saveConfiguration(configuration)
      }
      return PathFinder(store)
    }
  }

  private val storeScope = CoroutineScope(Dispatchers.IO)

  private var currentResolver: PathResolver = NotInitializedResolver

  private var _currentEnvironment: Environment? = null
  val currentEnvironment: Environment get() = _currentEnvironment
    ?: error("no current environment. Is PathFinder initialized?")

  private val listeners: MutableList<Listener> = mutableListOf()

  init {
    store.activeEnvironmentId()
      .onEach { environmentId ->
        val envId = environmentId
          ?: error("internal error: no active environment id")
        val environment = store.readEnvironmentById(envId)
          ?: error("internal error: no active environment by id = ${envId.value}")
        currentResolver = DefaultResolver(store, envId)
        _currentEnvironment = environment
        listeners.forEach { it.onEnvironmentSwitch(envId) }
      }
      .launchIn(storeScope)
  }

  suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String> = emptyMap(),
    queryParameters: Map<String, String> = emptyMap(),
  ): String {
    return currentResolver.buildUrl(id, pathVariables, queryParameters)
  }

  interface Listener {
    /**
     * Will be called on the environment change.
     *
     * WARNING: This method will be called on the background thread
     */
    fun onEnvironmentSwitch(environmentId: EnvironmentId)
  }

  fun switchEnvironment(environmentId: EnvironmentId) {
    storeScope.launch {
      store.changeActiveEnvironment(environmentId)
    }
  }

  fun replaceEnvironment(environment: Environment) {
    storeScope.launch {
      store.replaceEnvironment(environment)
    }
  }

  fun updateEachEnvironment(updater: (Environment) -> Environment) {
    storeScope.launch {
      store.readEnvironments().forEach {
        val new = updater(it)
        check(new.id == it.id) {
          "environment id changed during an update operation. This is not allowed. " +
            "Old id=${it.id}, new id=${new.id}"
        }
        store.replaceEnvironment(new)
      }
    }
  }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }
}

private val NotInitializedResolver = object : PathResolver {
  override suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String>,
    queryParameters: Map<String, String>,
  ): String {
    error("cannot resolve url: store not initialized. Wait for 'onEnvironmentSwitch' callback and then retry.")
  }
}
