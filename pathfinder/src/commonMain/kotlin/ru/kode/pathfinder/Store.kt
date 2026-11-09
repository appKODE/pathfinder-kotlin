package ru.kode.pathfinder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface Store {
  suspend fun saveConfiguration(configuration: Configuration)

  suspend fun replaceEnvironment(environment: Environment)

  suspend fun changeActiveEnvironment(id: EnvironmentId)
  fun activeEnvironmentId(): Flow<EnvironmentId?>
  suspend fun readActiveEnvironmentId(): EnvironmentId?

  fun environments(): Flow<List<Environment>>
  suspend fun readEnvironmentById(id: EnvironmentId): Environment?
  suspend fun readEnvironments(): List<Environment>

  fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>>
  suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): UrlConfiguration?

  fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>)
  fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>)

  // Fallbacks. The defaults do not persist them: PathFinder keeps them in memory until the process dies.

  /**
   * Replaces all stored fallback configurations.
   */
  suspend fun saveFallbackConfigurations(configurations: List<EnvironmentFallbackConfiguration>) = Unit
  fun fallbackConfigurations(): Flow<List<EnvironmentFallbackConfiguration>> = flowOf(emptyList())

  /**
   * Remembers [baseUrl] as the last one which served a request in region [regionCode].
   */
  suspend fun saveLastSuccessfulFallbackUrl(regionCode: String, baseUrl: String) = Unit

  /**
   * The last successful base url by region code.
   */
  fun lastSuccessfulFallbackUrls(): Flow<Map<String, String>> = flowOf(emptyMap())
}
