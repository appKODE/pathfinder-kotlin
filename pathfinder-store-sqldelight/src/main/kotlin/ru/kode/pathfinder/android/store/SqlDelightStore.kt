package ru.kode.pathfinder.android.store

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
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

  override fun saveConfiguration(configuration: Configuration) {
    val version = database.configurationQueries.findVersion().executeAsOneOrNull()?.version?.toInt()
    check(version == null || configuration.version >= version) {
      "cannot downgrade from $version to ${configuration.version}"
    }
    if (version == null || version < configuration.version) {
      if (version != null) {
        Log.d("pathfinder", "initializing configuration, moving from version $version to ${configuration.version}")
      } else {
        Log.d("pathfinder", "initializing configuration, version = ${configuration.version}")
      }
      replaceConfiguration(configuration)
    }
  }

  private fun replaceConfiguration(configuration: Configuration) {
    database.transaction {
      val previousEnvironmentId = database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()
      val activeEnvironmentId = configuration.environments.find { it.id.value == previousEnvironmentId }?.id
        ?: configuration.defaultEnvironmentId

      database.environmentQueries.deleteAll()

      database.configurationQueries.upsertVersion(
        version = configuration.version.toLong(),
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
        database.queryParameterQueries.insertOne(QueryParameter(configuration.id, name, value_ = ""))
      }
      spec.pathVariables.forEach { name ->
        database.pathVariableQueries.insertOne(PathVariable(configuration.id, name, value_ = ""))
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
      database.configurationQueries.findActiveEnvironment().executeAsOneOrNull()?.let(::EnvironmentId)
    }
  }

  override fun findEnvironments(): Flow<List<Environment>> {
    return database.environmentQueries
      .findAll(::mapToEnvironmentDomainModel)
      .asFlow()
      .map { it.executeAsList() }
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

  override suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): UrlConfiguration? {
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

  override fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>) {
    pathVariableValues.entries.forEach { (name, value) ->
      database.pathVariableQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value
      )
    }
  }

  override fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>) {
    queryParameterValues.entries.forEach { (name, value) ->
      database.queryParameterQueries.updateOne(
        name = name,
        value = value,
        urlConfigurationId = id.value
      )
    }
  }
}
