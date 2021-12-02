package ru.kode.pathfinder.android.store

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.android.AndroidSqliteDriver
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Query
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId
import ru.kode.pathfinder.android.store.adapter.StringListColumnAdapter
import ru.kode.pathfinder.android.store.mapper.mapToEnvironmentDomainModel
import ru.kode.pathfinder.android.store.mapper.toDomainModel
import ru.kode.pathfinder.android.store.mapper.toStorageModel
import ru.kode.pathfinder.android.store.mapper.toUrlConfigurationStorageModel
import ru.kode.pathfinder.map
import java.util.concurrent.CopyOnWriteArrayList
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
        database.queryParameterQueries.insertOne(QueryParameter(configuration.id, name, value = ""))
      }
      spec.pathVariables.forEach { name ->
        database.pathVariableQueries.insertOne(PathVariable(configuration.id, name, value = ""))
      }
    }
  }

  override fun changeActiveEnvironment(id: EnvironmentId) {
    database.configurationQueries.upsertActiveEnvironmentId(id.value)
  }

  override fun activeEnvironmentId(): Query<EnvironmentId?> {
    return database.configurationQueries
      .findActiveEnvironment()
      .toPathFinderQuery { rows -> rows.firstOrNull()?.let { EnvironmentId(it) } }
  }

  override fun findEnvironments(): Query<List<Environment>> {
    return database.environmentQueries
      .findAll(::mapToEnvironmentDomainModel)
      .toPathFinderQuery { rows -> rows }
  }

  override fun findEnvironmentById(id: EnvironmentId): Query<Environment?> {
    return database.environmentQueries
      .findById(id.value, ::mapToEnvironmentDomainModel)
      .toPathFinderQuery { rows -> rows.firstOrNull() }
  }

  override fun findUrlConfigurations(environmentId: EnvironmentId): Query<List<UrlConfiguration>> {
    return combineLatest(
      database.urlConfigurationQueries
        .findByEnvironmentId(environmentId.value)
        .toPathFinderQuery { it },
      database.pathVariableQueries.findAll()
        .toPathFinderQuery { it },
      database.queryParameterQueries.findAll()
        .toPathFinderQuery { it }
    ) { configurations, pathVariables, queryParameters ->
      configurations.map { configuration ->
        UrlConfiguration(
          id = UrlConfiguration.Id(configuration.id),
          pathTemplate = configuration.pathTemplate,
          name = configuration.name,
          httpMethod = configuration.httpMethod.toDomainModel(),
          pathVariableValues = pathVariables
            .filter { it.urlConfigurationId == configuration.id }
            .associateBy({ it.name }, { it.value }),
          queryParameterValues = queryParameters
            .filter { it.urlConfigurationId == configuration.id }
            .associateBy({ it.name }, { it.value }),
        )
      }
    }
  }

  override fun findUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId): Query<UrlConfiguration?> {
    return database.urlConfigurationQueries
      .findByEnvironmentAndSpecId(environmentId.value, urlSpecId.value)
      .toPathFinderQuery { it.firstOrNull() }
      .map { configurationSM ->
        if (configurationSM != null) {
          val pathVariables = database.pathVariableQueries
            .findByConfigurationId(configurationSM.id)
            .executeAsList()
          val queryParameters = database.queryParameterQueries
            .findByConfigurationId(configurationSM.id)
            .executeAsList()
          configurationSM.toDomainModel(pathVariables, queryParameters)
        } else null
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

internal class WrappedQuery<out RowType : Any, out ResultType>(
  private val sqlDelightQuery: com.squareup.sqldelight.Query<RowType>,
  private val mapper: (List<RowType>) -> ResultType,
) : Query<ResultType> {

  private val listeners = CopyOnWriteArrayList<SqlDelightListener>()

  class SqlDelightListener(val pathFinderListener: Query.Listener) : com.squareup.sqldelight.Query.Listener {
    override fun queryResultsChanged() {
      pathFinderListener.queryResultsChanged()
    }
  }

  override fun addListener(listener: Query.Listener) {
    val l = SqlDelightListener(pathFinderListener = listener)
    sqlDelightQuery.addListener(l)
    listeners.add(l)
  }

  override fun removeListener(listener: Query.Listener) {
    val l = listeners.find { it.pathFinderListener == listener }
    if (l != null) {
      sqlDelightQuery.removeListener(l)
    }
  }

  override fun execute(): ResultType {
    return mapper(sqlDelightQuery.executeAsList())
  }
}

private fun <RowType : Any, ResultType> com.squareup.sqldelight.Query<RowType>.toPathFinderQuery(
  mapper: (List<RowType>) -> ResultType,
): Query<ResultType> {
  return WrappedQuery(this, mapper)
}
