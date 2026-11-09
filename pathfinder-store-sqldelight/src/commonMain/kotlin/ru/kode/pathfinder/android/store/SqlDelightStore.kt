package ru.kode.pathfinder.android.store

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentFallbackConfiguration
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId
import ru.kode.pathfinder.android.store.adapter.StringListColumnAdapter
import ru.kode.pathfinder.android.store.adapter.StringMapColumnAdapter
import ru.kode.pathfinder.android.store.mapper.mapToEnvironmentDomainModel
import ru.kode.pathfinder.android.store.mapper.toDomainModel
import ru.kode.pathfinder.android.store.mapper.toStorageModel
import ru.kode.pathfinder.android.store.mapper.toUrlConfigurationStorageModel
import ru.kode.pathfinder.android.store.Environment as EnvironmentStorageModel
import ru.kode.pathfinder.android.store.FallbackConfiguration as FallbackConfigurationStorageModel

/**
 * [Store] backed by SQLDelight. Create the [driver] with [PathFinderDatabase.Schema],
 * or use the platform factory `SqlDelightStore(context)` (Android) / `SqlDelightStore()` (iOS).
 */
class SqlDelightStore(driver: SqlDriver) : Store {

  private val database = PathFinderDatabase(
    driver,
    EnvironmentAdapter = EnvironmentStorageModel.Adapter(
      queryParametersAdapter = StringListColumnAdapter(separator = ":::"),
      additionalBaseUrlsAdapter = StringMapColumnAdapter(),
    ),
  )

  override suspend fun saveConfiguration(configuration: Configuration) {
    withContext(Dispatchers.IO) {
      val version = database.configurationQueries.findVersion().executeAsOneOrNull()?.version
      val newVersion = configuration.computeChecksum()
      if (version == null || version != newVersion) {
        if (version != null) {
          println("pathfinder: initializing new configuration, replacing after change")
        } else {
          println("pathfinder: initializing new configuration")
        }
        replaceConfiguration(configuration, newVersion)
      }
    }
  }

  // Does not touch the configuration version: it always is a checksum of the declared configuration,
  // so runtime changes survive app restarts until the declared configuration changes.
  override suspend fun replaceEnvironment(environment: Environment) {
    withContext(Dispatchers.IO) {
      database.transaction {
        val isNew = database.environmentQueries.findById(environment.id.value).executeAsOneOrNull() == null
        database.environmentQueries.insertOne(environment.toStorageModel())
        if (isNew) {
          val urlSpecs = database.urlConfigurationQueries
            .findAllSpecs { pathTemplate, name, httpMethod, baseUrlKey ->
              UrlSpec(
                pathTemplate = pathTemplate,
                httpMethod = httpMethod.toDomainModel(),
                name = name,
                baseUrlKey = baseUrlKey,
              )
            }
            .executeAsList()
          saveInitialUrlConfigurations(environment, urlSpecs)
        }
      }
    }
  }

  private fun replaceConfiguration(configuration: Configuration, newVersion: String) {
    database.transaction {
      val previousEnvironmentId =
        database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
      val activeEnvironmentId =
        configuration.environments.find { it.id.value == previousEnvironmentId }?.id
          ?: configuration.defaultEnvironmentId

      database.environmentQueries.deleteAll()

      database.configurationQueries.upsertVersion(
        version = newVersion,
        activeEnvironmentId = activeEnvironmentId.value
      )
      configuration.environments.forEach { environment ->
        database.environmentQueries.insertOne(environment.toStorageModel())
        saveInitialUrlConfigurations(environment, configuration.urlSpecs)
      }
    }
  }

  private fun saveInitialUrlConfigurations(environment: Environment, urlSpecs: List<UrlSpec>) {
    urlSpecs.forEach { spec ->
      val configuration = spec.toUrlConfigurationStorageModel(environment.id)
      database.urlConfigurationQueries.insertOne(configuration)
      environment.queryParameters?.forEach { name ->
        database.queryParameterQueries.insertOne(
          QueryParameter(
            configuration.id,
            name,
            value_ = "",
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis(),
          )
        )
      }
      spec.pathVariables.forEach { name ->
        database.pathVariableQueries.insertOne(
          PathVariable(
            configuration.id,
            name,
            value_ = "",
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis(),
          )
        )
      }
    }
  }

  override suspend fun changeActiveEnvironment(id: EnvironmentId) {
    withContext(Dispatchers.IO) {
      database.configurationQueries.upsertActiveEnvironmentId(id.value)
    }
  }

  override fun activeEnvironmentId(): Flow<EnvironmentId?> = database.configurationQueries
    .findActiveEnvironment()
    .asFlow()
    .map { it.executeAsOneOrNull()?.let(::EnvironmentId) }

  override suspend fun readActiveEnvironmentId(): EnvironmentId? = withContext(Dispatchers.IO) {
    database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
      ?.let(::EnvironmentId)
  }

  override fun environments(): Flow<List<Environment>> = database.environmentQueries
    .findAll(::mapToEnvironmentDomainModel)
    .asFlow()
    .map { it.executeAsList() }

  override suspend fun readEnvironments(): List<Environment> = withContext(Dispatchers.IO) {
    database.environmentQueries
      .findAll(::mapToEnvironmentDomainModel)
      .executeAsList()
  }

  override suspend fun readEnvironmentById(id: EnvironmentId): Environment? = withContext(Dispatchers.IO) {
    database.environmentQueries
      .findById(id.value, ::mapToEnvironmentDomainModel)
      .executeAsOneOrNull()
  }

  override fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>> = combine(
    database.urlConfigurationQueries
      .findByEnvironmentId(environmentId.value).asFlow().map { it.executeAsList() },
    database.pathVariableQueries.findAll().asFlow().map { it.executeAsList() },
    database.queryParameterQueries.findAll().asFlow().map { it.executeAsList() },
  ) { configurations, pathVariables, queryParameters ->
    configurations.map { configuration ->
      UrlConfiguration(
        id = UrlConfiguration.Id(configuration.id),
        pathTemplate = configuration.pathTemplate,
        name = configuration.name,
        httpMethod = configuration.httpMethod.toDomainModel(),
        pathVariableValues = pathVariables
          .filter { it.urlConfigurationId == configuration.id }
          .associateBy({ it.name }, { it.value_ }),
        queryParameterValues = queryParameters
          .filter { it.urlConfigurationId == configuration.id }
          .associateBy({ it.name }, { it.value_ }),
        baseUrlKey = configuration.baseUrlKey,
      )
    }
  }

  override suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): UrlConfiguration? =
    withContext(Dispatchers.IO) {
      val configurationSM = database.urlConfigurationQueries
        .findByEnvironmentAndSpecId(environmentId.value, urlSpecId.value)
        .executeAsOneOrNull()
      if (configurationSM != null) {
        val pathVariables = database.pathVariableQueries
          .findByConfigurationId(configurationSM.id)
          .executeAsList()
        val queryParameters = database.queryParameterQueries
          .findByConfigurationId(configurationSM.id)
          .executeAsList()
        configurationSM.toDomainModel(pathVariables, queryParameters)
      } else {
        null
      }
    }

  override fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>) {
    pathVariableValues.entries.forEach { (name, value) ->
      database.pathVariableQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value,
        updatedAt = currentTimeMillis(),
      )
    }
  }

  override fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>) {
    queryParameterValues.entries.forEach { (name, value) ->
      database.queryParameterQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value,
        updatedAt = currentTimeMillis(),
      )
    }
  }

  override suspend fun saveFallbackConfigurations(configurations: List<EnvironmentFallbackConfiguration>) {
    withContext(Dispatchers.IO) {
      database.transaction {
        database.fallbackConfigurationQueries.deleteAll()
        configurations.forEach {
          database.fallbackConfigurationQueries.insertOne(
            FallbackConfigurationStorageModel(it.key, it.baseUrls.joinToString("\n"), it.requestTimeout)
          )
        }
      }
    }
  }

  override fun fallbackConfigurations(): Flow<List<EnvironmentFallbackConfiguration>> =
    database.fallbackConfigurationQueries
      .findAll { key, baseUrls, requestTimeout ->
        EnvironmentFallbackConfiguration(key, baseUrls.split("\n").filter { it.isNotEmpty() }, requestTimeout)
      }
      .asFlow()
      .map { it.executeAsList() }

  override suspend fun saveLastSuccessfulFallbackUrl(key: String, baseUrl: String) {
    withContext(Dispatchers.IO) {
      database.lastSuccessfulFallbackQueries.upsert(LastSuccessfulFallback(key, baseUrl))
    }
  }

  override fun lastSuccessfulFallbackUrls(): Flow<Map<String, String>> = database.lastSuccessfulFallbackQueries
    .findAll()
    .asFlow()
    .map { query -> query.executeAsList().associate { it.fallbackKey to it.baseUrl } }
}
