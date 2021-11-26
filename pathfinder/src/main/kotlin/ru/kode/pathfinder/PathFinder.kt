package ru.kode.pathfinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PathFinder private constructor(
  val store: Store,
) {

  companion object {
    suspend fun create(store: Store, configuration: Configuration): PathFinder {
      val pathFinder = PathFinder(store)
      withContext(Dispatchers.IO) {
        store.saveConfiguration(configuration)
        pathFinder.updateResolverOnEnvironmentSwitch()
      }
      return pathFinder
    }
  }

  private val storeScope = CoroutineScope(Dispatchers.IO)

  private var currentResolver: PathResolver = NotInitializedResolver

  private var _currentEnvironment: Environment? = null
  val currentEnvironment: Environment get() = _currentEnvironment
    ?: error("no current environment. Is PathFinder initialized?")

  private val listeners: MutableList<Listener> = mutableListOf()

  init {
    store.activeEnvironmentId().addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        storeScope.launch {
          updateResolverOnEnvironmentSwitch()
        }
      }
    })
  }

  suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String> = emptyMap(),
    queryParameters: Map<String, String> = emptyMap(),
  ): String {
    return currentResolver.buildUrl(id, pathVariables, queryParameters)
  }

  private fun updateResolverOnEnvironmentSwitch() {
    val environmentId = store.activeEnvironmentId().execute()
      ?: error("internal error: no active environment id")
    val environment = store.findEnvironmentById(environmentId).execute()
      ?: error("internal error: no active environment by id = ${environmentId.value}")
    currentResolver = DefaultResolver(store, environmentId)
    _currentEnvironment = environment
    listeners.forEach { it.onEnvironmentSwitch(environmentId) }
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
