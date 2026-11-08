package ru.kode.pathfinder.android.ui.compose

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId

/**
 * Copy of the core test FakeStore. In-memory [Store] which follows the contract of the SQLDelight store: stored
 * path variables and query parameters exist for every declared name with an empty value until they are overridden.
 */
internal class FakeStore : Store {
  // one state, so that observers never see environments and the active id out of sync (the real store uses transactions)
  private val state = MutableStateFlow(State())
  private val overrides = MutableStateFlow<Map<UrlConfiguration.Id, Overrides>>(emptyMap())
  private var specs = emptyList<UrlSpec>()

  private data class State(val environments: List<Environment> = emptyList(), val activeId: EnvironmentId? = null)

  private data class Overrides(
    val pathVariables: Map<String, String> = emptyMap(),
    val queryParameters: Map<String, String> = emptyMap(),
  )

  override suspend fun saveConfiguration(configuration: Configuration) {
    specs = configuration.urlSpecs
    overrides.value = emptyMap()
    state.update { state ->
      State(
        environments = configuration.environments,
        activeId = state.activeId.takeIf { id -> configuration.environments.any { it.id == id } }
          ?: configuration.defaultEnvironmentId,
      )
    }
  }

  override suspend fun replaceEnvironment(environment: Environment) {
    val current = state.value.environments
    // widens the window between a read and this write, so that unsynchronized updates would get lost
    yield()
    val environments = if (current.any { it.id == environment.id }) {
      current.map { if (it.id == environment.id) environment else it }
    } else {
      current + environment
    }
    state.update { it.copy(environments = environments) }
  }

  override suspend fun changeActiveEnvironment(id: EnvironmentId) {
    state.update { it.copy(activeId = id) }
  }

  override fun activeEnvironmentId(): Flow<EnvironmentId?> = state.map { it.activeId }.distinctUntilChanged()
  override suspend fun readActiveEnvironmentId(): EnvironmentId? = state.value.activeId
  override fun environments(): Flow<List<Environment>> = state.map { it.environments }.distinctUntilChanged()
  override suspend fun readEnvironmentById(id: EnvironmentId) = state.value.environments.find { it.id == id }
  override suspend fun readEnvironments() = state.value.environments

  override fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>> =
    combine(state, overrides) { _, _ -> specs.map { it.toConfiguration(environmentId) } }

  override suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): UrlConfiguration? {
    val environment = state.value.environments.find { it.id == environmentId } ?: return null
    return specs.find { it.id == urlSpecId }?.toConfiguration(environment.id)
  }

  override fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>) {
    overrides.value += id to overridesOf(id).let { it.copy(pathVariables = it.pathVariables + pathVariableValues) }
  }

  override fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>) {
    overrides.value +=
      id to overridesOf(id).let { it.copy(queryParameters = it.queryParameters + queryParameterValues) }
  }

  private fun overridesOf(id: UrlConfiguration.Id) = overrides.value[id] ?: Overrides()

  private fun UrlSpec.toConfiguration(environmentId: EnvironmentId): UrlConfiguration {
    val id = UrlConfiguration.Id("${environmentId.value}_${id.value}")
    val stored = overridesOf(id)
    val queryParameterNames = state.value.environments.find { it.id == environmentId }?.queryParameters.orEmpty()
    return UrlConfiguration(
      id = id,
      pathTemplate = pathTemplate,
      name = name,
      httpMethod = httpMethod,
      pathVariableValues = pathVariables.associateWith { "" } + stored.pathVariables,
      queryParameterValues = queryParameterNames.associateWith { "" } + stored.queryParameters,
      baseUrlKey = baseUrlKey,
    )
  }
}
