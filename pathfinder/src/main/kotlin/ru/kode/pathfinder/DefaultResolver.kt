package ru.kode.pathfinder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultResolver(
  private val store: Store,
  private val environmentId: EnvironmentId,
) : PathResolver {

  override suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String>,
    queryParameters: Map<String, String>,
  ): String {
    return withContext(Dispatchers.IO) {
      val configuration = store.findUrlConfiguration(urlSpecId = id, environmentId = environmentId)
        .execute()
        // this resolver must be created only after initial population, if not → it is an error
        ?: error("internal error: no url configuration for urlSpecId = ${id.value}")

      checkAllPathVariablesPresent(configuration, pathVariables)

      val environment = store.findEnvironmentById(environmentId)
        .execute()
        // this resolver must be created only after initial population, if not → it is an error
        ?: error("internal error: no environment configuration for environmentId = ${environmentId.value}")

      buildUrl(
        environment.baseUrl,
        configuration.pathTemplate,
        // values saved in store have higher precedence, so they are applied last: to override external ones
        pathVariables.plus(configuration.pathVariableValues.filterValues { it.isNotBlank() }),
        queryParameters.plus(configuration.queryParameterValues.filterValues { it.isNotBlank() }),
      )
    }
  }

  private fun buildUrl(
    baseUrl: String,
    pathTemplate: String,
    pathVariables: Map<String, String>,
    queryParameters: Map<String, String>,
  ): String {
    val path = "$baseUrl/" + pathVariables.entries.fold(pathTemplate, { template, (name, value) ->
      template.replace("{$name}", value)
    })
    val nonEmptyParams = queryParameters.filterValues { it.isNotBlank() }
    return if (nonEmptyParams.isNotEmpty()) {
      buildString {
        append(path)
        append('?')
        nonEmptyParams.entries.forEachIndexed { index, (name, value) ->
          if (index != 0) append('&')
          append(name)
          append('=')
          append(value)
        }
      }
    } else path
  }

  private fun checkAllPathVariablesPresent(configuration: UrlConfiguration, pathVariables: Map<String, String>) {
    val missing = configuration.pathVariables.minus(pathVariables.keys)
    check(missing.count() == 0) {
      "not all path variables specified. Missing: ${missing.joinToString(prefix = "\"", postfix = "\"")}"
    }
  }
}
