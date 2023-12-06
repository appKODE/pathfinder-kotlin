package ru.kode.pathfinder

import kotlinx.coroutines.flow.Flow

interface Store {
  suspend fun saveConfiguration(configuration: Configuration)

  suspend fun replaceEnvironment(environment: Environment)

  fun changeActiveEnvironment(id: EnvironmentId)
  fun activeEnvironmentId(): Flow<EnvironmentId?>
  suspend fun readActiveEnvironmentId(): EnvironmentId?

  fun findEnvironments(): Flow<List<Environment>>
  suspend fun readEnvironmentById(id: EnvironmentId): Environment?
  suspend fun readEnvironments(): List<Environment>

  fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>>
  suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): UrlConfiguration?

  fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>)
  fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>)
}
