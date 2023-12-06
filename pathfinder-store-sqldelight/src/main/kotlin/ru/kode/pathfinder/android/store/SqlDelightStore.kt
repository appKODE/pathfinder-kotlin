package ru.kode.pathfinder.android.store

import android.content.Context
import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId
import ru.kode.pathfinder.android.store.adapter.StringListColumnAdapter
import ru.kode.pathfinder.android.store.mapper.mapToEnvironmentDomainModel
import ru.kode.pathfinder.android.store.mapper.toDomainModel
import ru.kode.pathfinder.android.store.mapper.toStorageModel
import ru.kode.pathfinder.android.store.mapper.toUrlConfigurationStorageModel
import ru.kode.pathfinder.android.store.Environment as EnvironmentStorageModel

class SqlDelightStore(context: Context) : Store {

  private val database = createDatabase(context)

  private fun createDatabase(context: Context): PathFinderDatabase {
    val driver = AndroidSqliteDriver(
      PathFinderDatabase.Schema,
      context,
      name = "pathfinder.db"
    )
    return PathFinderDatabase(
      driver,
      EnvironmentAdapter = EnvironmentStorageModel.Adapter(
        queryParametersAdapter = StringListColumnAdapter(separator = ":::")
      )
    )
  }

  override suspend fun saveConfiguration(configuration: Configuration) {
    withContext(Dispatchers.IO) {
      val version = database.configurationQueries.findVersion().executeAsOneOrNull()?.version
      val newVersion = configuration.computeChecksum()
      if (version == null || version != newVersion) {
        if (version != null) {
          Log.d(
            "pathfinder",
            "initializing new configuration, replacing after change"
          )
        } else {
          Log.d("pathfinder", "initializing new configuration")
        }
        replaceConfiguration(configuration, newVersion)
      }
    }
  }

  override suspend fun replaceEnvironment(environment: Environment) {
    withContext(Dispatchers.IO) {
      database.transaction {
        database.environmentQueries.insertOne(environment.toStorageModel())
        val newVersion = recomputeVersionFromDB()
        val activeEnvironmentId =
          database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
            ?: error("no active environment, has PathFinder been initialized?")
        database.configurationQueries.upsertVersion(newVersion, activeEnvironmentId)
      }
    }
  }

  private fun recomputeVersionFromDB(): String {
    return database.transactionWithResult {
      val environments = database.environmentQueries
        .findAll { id, name, baseUrl, queryParameters ->
          Environment(
            id = EnvironmentId(id),
            name = name,
            baseUrl = baseUrl,
            queryParameters = queryParameters?.toSet()
          )
        }
        .executeAsList()
      val urlSpecs = database.urlConfigurationQueries
        .findAll { pathTemplate, name, httpMethod ->
          UrlSpec(pathTemplate = pathTemplate, httpMethod = httpMethod.toDomainModel(), name = name)
        }
        .executeAsList()

      val activeEnvironmentId =
        database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
          ?: error("no active environment, has PathFinder been initialized?")

      val configuration = Configuration(
        environments = environments,
        urlSpecs = urlSpecs,
        defaultEnvironmentId = EnvironmentId(activeEnvironmentId)
      )
      configuration.computeChecksum()
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
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
          )
        )
      }
      spec.pathVariables.forEach { name ->
        database.pathVariableQueries.insertOne(
          PathVariable(
            configuration.id,
            name,
            value_ = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
          )
        )
      }
    }
  }

  override fun changeActiveEnvironment(id: EnvironmentId) {
    database.configurationQueries.upsertActiveEnvironmentId(id.value)
  }

  override fun activeEnvironmentId(): Flow<EnvironmentId?> {
    return database.configurationQueries
      .findActiveEnvironment()
      .asFlow()
      .map { it.executeAsOneOrNull()?.let(::EnvironmentId) }
  }

  override suspend fun readActiveEnvironmentId(): EnvironmentId? {
    return withContext(Dispatchers.IO) {
      database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
        ?.let(::EnvironmentId)
    }
  }

  override fun findEnvironments(): Flow<List<Environment>> {
    return database.environmentQueries
      .findAll(::mapToEnvironmentDomainModel)
      .asFlow()
      .map { it.executeAsList() }
  }

  override suspend fun readEnvironments(): List<Environment> {
    return withContext(Dispatchers.IO) {
      database.environmentQueries
        .findAll(::mapToEnvironmentDomainModel)
        .executeAsList()
    }
  }

  override suspend fun readEnvironmentById(id: EnvironmentId): Environment? {
    return withContext(Dispatchers.IO) {
      database.environmentQueries
        .findById(id.value, ::mapToEnvironmentDomainModel)
        .executeAsOneOrNull()
    }
  }

  override fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>> {
    return combine(
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
        )
      }
    }
  }

  override suspend fun readUrlConfiguration(
    urlSpecId: UrlSpecId,
    environmentId: EnvironmentId
  ): UrlConfiguration? {
    return withContext(Dispatchers.IO) {
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
  }

  override fun updatePathVariables(
    id: UrlConfiguration.Id,
    pathVariableValues: Map<String, String>
  ) {
    pathVariableValues.entries.forEach { (name, value) ->
      database.pathVariableQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value,
        updatedAt = System.currentTimeMillis(),
      )
    }
  }

  override fun updateQueryParameters(
    id: UrlConfiguration.Id,
    queryParameterValues: Map<String, String>
  ) {
    queryParameterValues.entries.forEach { (name, value) ->
      database.queryParameterQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value,
        updatedAt = System.currentTimeMillis(),
      )
    }
  }
}
