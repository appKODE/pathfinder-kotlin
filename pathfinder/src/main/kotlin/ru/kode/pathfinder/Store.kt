package ru.kode.pathfinder

interface Store {
  fun saveConfiguration(configuration: Configuration)

  fun changeActiveEnvironment(id: EnvironmentId)
  fun activeEnvironmentId(): Query<EnvironmentId?>

  fun findEnvironments(): Query<List<Environment>>
  fun findEnvironmentById(id: EnvironmentId): Query<Environment?>

  fun findUrlConfigurations(environmentId: EnvironmentId): Query<List<UrlConfiguration>>
  fun findUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): Query<UrlConfiguration?>

  fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>)
  fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>)
}
