package ru.kode.pathfinder.android.store.mapper

import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.UrlConfiguration as UrlConfigurationDomainModel
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.android.store.FindByEnvironmentAndSpecId
import ru.kode.pathfinder.android.store.UrlConfiguration
import ru.kode.pathfinder.android.store.Environment as EnvironmentStorageModel
import ru.kode.pathfinder.android.store.pathVariable.FindByConfigurationId as FindPathVarByConfigurationId
import ru.kode.pathfinder.android.store.queryParameter.FindByConfigurationId as FindQueryVarByConfigurationId

internal fun Environment.toStorageModel(): EnvironmentStorageModel {
  return EnvironmentStorageModel(
    id = this.id.value,
    name = this.name,
    baseUrl = this.baseUrl,
    queryParameters = this.queryParameters?.toList()
  )
}

internal fun UrlSpec.toUrlConfigurationStorageModel(environmentId: EnvironmentId): UrlConfiguration {
  return UrlConfiguration(
    id = createStorageId(environmentId),
    specId = this.id.value,
    environmentId = environmentId.value,
    pathTemplate = this.pathTemplate,
    name = this.name,
    httpMethod = this.httpMethod.toStorageModel(),
  )
}

private fun UrlSpec.createStorageId(environmentId: EnvironmentId): String {
  return buildString {
    append(this@createStorageId.id.value)
    append('_')
    append(environmentId.value)
  }
}

internal fun mapToEnvironmentDomainModel(
  id: String,
  name: String,
  baseUrl: String,
  queryParameters: List<String>?,
): Environment {
  return Environment(
    id = EnvironmentId(id),
    name = name,
    baseUrl = baseUrl,
    queryParameters = queryParameters?.toSet()
  )
}

internal fun FindByEnvironmentAndSpecId.toDomainModel(
  pathVariables: List<FindPathVarByConfigurationId>,
  queryParameters: List<FindQueryVarByConfigurationId>,
): ru.kode.pathfinder.UrlConfiguration {
  return UrlConfigurationDomainModel(
    id = UrlConfigurationDomainModel.Id(id),
    pathTemplate = pathTemplate,
    name = name,
    httpMethod = this.httpMethod.toDomainModel(),
    pathVariableValues = pathVariables.associateBy({ it.name }, { it.value }),
    queryParameterValues = queryParameters.associateBy({ it.name }, { it.value }),
  )
}

internal fun UrlSpec.HttpMethod.toStorageModel(): String {
  return when (this) {
    UrlSpec.HttpMethod.GET -> "GET"
    UrlSpec.HttpMethod.POST -> "POST"
    UrlSpec.HttpMethod.PUT -> "PUT"
    UrlSpec.HttpMethod.UPDATE -> "UPDATE"
    UrlSpec.HttpMethod.PATCH -> "PATCH"
    UrlSpec.HttpMethod.DELETE -> "DELETE"
  }
}

internal fun String.toDomainModel(): UrlSpec.HttpMethod {
  return when (this) {
    "GET" -> UrlSpec.HttpMethod.GET
    "POST" -> UrlSpec.HttpMethod.POST
    "PUT" -> UrlSpec.HttpMethod.PUT
    "UPDATE" -> UrlSpec.HttpMethod.UPDATE
    "PATCH" -> UrlSpec.HttpMethod.PATCH
    "DELETE" -> UrlSpec.HttpMethod.DELETE
    else -> error("unknown http method code $this")
  }
}
