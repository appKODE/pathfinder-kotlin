package ru.kode.pathfinder.android.store

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentFallbackConfiguration
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.PathFinder
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlDelightStoreTest {
  private val dev = Environment(
    id = EnvironmentId("dev"),
    name = "Dev",
    baseUrl = "https://dev.example.com",
    queryParameters = setOf("lang", "token"),
    additionalBaseUrls = mapOf("auth" to "https://auth.dev.example.com", "cdn" to "https://cdn.dev.example.com"),
  )
  private val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://example.com")
  private val users = UrlSpec(pathTemplate = "users/{id}", httpMethod = UrlSpec.HttpMethod.GET, name = "User")
  private val login = UrlSpec(
    pathTemplate = "login",
    httpMethod = UrlSpec.HttpMethod.POST,
    name = "Login",
    baseUrlKey = "auth",
  )
  private val configuration = Configuration(
    environments = listOf(dev, prod),
    urlSpecs = listOf(users, login),
    defaultEnvironmentId = dev.id,
  )

  private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PathFinderDatabase.Schema.create(it) }
  private val store = SqlDelightStore(driver)

  @Test
  fun `saveConfiguration on an empty database creates everything`() = runTest {
    store.saveConfiguration(configuration)

    assertEquals(listOf(dev, prod), store.readEnvironments())
    assertEquals(dev.id, store.readActiveEnvironmentId())
    assertEquals(4L, driver.count("UrlConfiguration"))

    val devUsers = assertNotNull(store.readUrlConfiguration(users.id, dev.id))
    assertEquals("users/{id}", devUsers.pathTemplate)
    assertEquals("User", devUsers.name)
    assertEquals(UrlSpec.HttpMethod.GET, devUsers.httpMethod)
    assertEquals(mapOf("id" to ""), devUsers.pathVariableValues)
    assertEquals(mapOf("lang" to "", "token" to ""), devUsers.queryParameterValues)
    assertNull(devUsers.baseUrlKey)

    val prodLogin = assertNotNull(store.readUrlConfiguration(login.id, prod.id))
    assertEquals(emptyMap(), prodLogin.pathVariableValues)
    assertEquals(emptyMap(), prodLogin.queryParameterValues)
    assertEquals("auth", prodLogin.baseUrlKey)
  }

  @Test
  fun `saving the same configuration again keeps overrides`() = runTest {
    store.saveConfiguration(configuration)
    val id = store.readUrlConfiguration(users.id, dev.id)!!.id
    store.updatePathVariables(id, mapOf("id" to "42"))
    store.updateQueryParameters(id, mapOf("lang" to "en"))

    store.saveConfiguration(configuration)

    val stored = store.readUrlConfiguration(users.id, dev.id)!!
    assertEquals(mapOf("id" to "42"), stored.pathVariableValues)
    assertEquals(mapOf("lang" to "en", "token" to ""), stored.queryParameterValues)
  }

  @Test
  fun `a changed configuration replaces everything and keeps the active environment`() = runTest {
    store.saveConfiguration(configuration)
    store.changeActiveEnvironment(prod.id)
    val id = store.readUrlConfiguration(users.id, prod.id)!!.id
    store.updatePathVariables(id, mapOf("id" to "42"))

    store.saveConfiguration(configuration.copy(urlSpecs = listOf(users)))

    assertEquals(prod.id, store.readActiveEnvironmentId())
    assertEquals(mapOf("id" to ""), store.readUrlConfiguration(users.id, prod.id)!!.pathVariableValues)
    assertNull(store.readUrlConfiguration(login.id, prod.id))
  }

  @Test
  fun `a changed configuration without the active environment falls back to the default`() = runTest {
    store.saveConfiguration(configuration)
    store.changeActiveEnvironment(prod.id)

    store.saveConfiguration(configuration.copy(environments = listOf(dev)))

    assertEquals(dev.id, store.readActiveEnvironmentId())
    assertEquals(listOf(dev), store.readEnvironments())
    assertEquals(2L, driver.count("UrlConfiguration"))
  }

  @Test
  fun `replaced environment and overrides survive an app restart with the same declared configuration`() = runTest {
    store.saveConfiguration(configuration)
    val id = store.readUrlConfiguration(users.id, dev.id)!!.id
    store.updatePathVariables(id, mapOf("id" to "42"))
    store.replaceEnvironment(dev.copy(baseUrl = "https://dynamic.example.com"))

    // app restart: PathFinder.create saves the declared configuration again
    store.saveConfiguration(configuration)

    assertEquals("https://dynamic.example.com", store.readEnvironmentById(dev.id)!!.baseUrl)
    assertEquals(mapOf("id" to "42"), store.readUrlConfiguration(users.id, dev.id)!!.pathVariableValues)
  }

  @Test
  fun `replaceEnvironment with a new id creates url configurations for every spec`() = runTest {
    store.saveConfiguration(configuration)
    val stage = Environment(
      id = EnvironmentId("stage"),
      name = "Stage",
      baseUrl = "https://stage.example.com",
      queryParameters = setOf("lang"),
    )

    store.replaceEnvironment(stage)

    assertEquals(listOf(dev, prod, stage), store.readEnvironments())
    val stageUsers = assertNotNull(store.readUrlConfiguration(users.id, stage.id))
    assertEquals(mapOf("id" to ""), stageUsers.pathVariableValues)
    assertEquals(mapOf("lang" to ""), stageUsers.queryParameterValues)
    assertEquals("auth", assertNotNull(store.readUrlConfiguration(login.id, stage.id)).baseUrlKey)
    assertEquals(6L, driver.count("UrlConfiguration"))
  }

  @Test
  fun `replaceEnvironment with an existing id does not duplicate rows`() = runTest {
    store.saveConfiguration(configuration)
    val urlRows = driver.count("UrlConfiguration")
    val pathVariableRows = driver.count("PathVariable")
    val queryParameterRows = driver.count("QueryParameter")

    store.replaceEnvironment(dev.copy(name = "Renamed"))

    assertEquals("Renamed", store.readEnvironmentById(dev.id)!!.name)
    assertEquals(2, store.readEnvironments().size)
    assertEquals(urlRows, driver.count("UrlConfiguration"))
    assertEquals(pathVariableRows, driver.count("PathVariable"))
    assertEquals(queryParameterRows, driver.count("QueryParameter"))
  }

  @Test
  fun `environment fields round trip`() = runTest {
    store.saveConfiguration(configuration)

    store.replaceEnvironment(prod.copy(queryParameters = setOf("x"), additionalBaseUrls = mapOf("a" to "https://a=b")))
    store.readEnvironmentById(prod.id)!!.let {
      assertEquals(setOf("x"), it.queryParameters)
      assertEquals(mapOf("a" to "https://a=b"), it.additionalBaseUrls)
    }

    store.replaceEnvironment(prod)
    val stored = store.readEnvironmentById(prod.id)!!
    assertEquals(emptyMap(), stored.additionalBaseUrls)
    assertNull(stored.queryParameters)
    assertNull(driver.queryString("SELECT additionalBaseUrls FROM Environment WHERE id = 'prod'"))
  }

  @Test
  fun `changeActiveEnvironment updates the active id`() = runTest {
    store.saveConfiguration(configuration)

    store.changeActiveEnvironment(prod.id)

    assertEquals(prod.id, store.readActiveEnvironmentId())
  }

  @Test
  fun `readUrlConfiguration and readEnvironmentById of unknown ids return null`() = runTest {
    store.saveConfiguration(configuration)

    assertNull(store.readUrlConfiguration(UrlSpecId("GET_unknown"), dev.id))
    assertNull(store.readUrlConfiguration(users.id, EnvironmentId("unknown")))
    assertNull(store.readEnvironmentById(EnvironmentId("unknown")))
  }

  @Test
  fun `flows emit after changes`() = runTest {
    assertNull(store.activeEnvironmentId().awaitFirst { true })
    store.saveConfiguration(configuration)

    store.changeActiveEnvironment(prod.id)
    store.activeEnvironmentId().awaitFirst { it == prod.id }

    store.replaceEnvironment(prod.copy(name = "Renamed"))
    store.environments().awaitFirst { environments -> environments.any { it.name == "Renamed" } }

    val id = store.readUrlConfiguration(users.id, dev.id)!!.id
    store.updatePathVariables(id, mapOf("id" to "42"))
    store.updateQueryParameters(id, mapOf("lang" to "en"))
    val urls = store.urlConfigurations(dev.id).awaitFirst { urls ->
      urls.any { it.pathVariableValues["id"] == "42" && it.queryParameterValues["lang"] == "en" }
    }
    assertEquals(listOf(users.id.value, login.id.value), urls.map { "${it.httpMethod}_${it.pathTemplate}" })
    assertEquals(listOf(null, "auth"), urls.map { it.baseUrlKey })
    assertEquals(mapOf("lang" to "en", "token" to ""), urls.first().queryParameterValues)
  }

  @Test
  fun `PathFinder works on top of the store`() = runTest {
    val pathFinder = PathFinder.create(store, configuration)

    assertEquals("https://dev.example.com/users/1", pathFinder.buildUrl(users.id, mapOf("id" to "1")))
    assertEquals("https://auth.dev.example.com/login", pathFinder.buildUrl(login.id))

    pathFinder.updateEnvironment(prod.id) { it.copy(baseUrl = "https://dynamic.example.com") }
    pathFinder.switchEnvironment(prod.id)

    assertEquals(prod.id, pathFinder.currentEnvironment.id)
    assertEquals("https://dynamic.example.com/users/1", pathFinder.buildUrl(users.id, mapOf("id" to "1")))

    // restart keeps the runtime change
    val restarted = PathFinder.create(store, configuration)
    assertEquals("https://dynamic.example.com", restarted.currentEnvironment.baseUrl)
  }

  @Test
  fun `fallback configurations and last successful urls round trip`() = runTest {
    val configA = EnvironmentFallbackConfiguration("A", listOf("https://a0.test", "https://a1.test"), 1000)
    val empty = EnvironmentFallbackConfiguration("B", emptyList(), 2000)

    store.saveFallbackConfigurations(listOf(configA, empty))
    assertEquals(setOf(configA, empty), store.fallbackConfigurations().first().toSet())
    store.saveFallbackConfigurations(listOf(configA))
    assertEquals(listOf(configA), store.fallbackConfigurations().first())

    store.saveLastSuccessfulFallbackUrl("A", "https://a0.test")
    store.saveLastSuccessfulFallbackUrl("A", "https://a1.test")
    store.saveLastSuccessfulFallbackUrl("B", "https://b.test")
    assertEquals(
      mapOf("A" to "https://a1.test", "B" to "https://b.test"),
      store.lastSuccessfulFallbackUrls().first()
    )
  }

  @Test
  fun `PathFinder fallbacks and markFallbackSuccess survive a restart`() = runTest {
    val pathFinder = PathFinder.create(store, configuration)
    pathFinder.updateFallbackEnvironments(
      listOf(EnvironmentFallbackConfiguration("A", listOf("https://a0.test", "https://a1.test"), 1000))
    )
    pathFinder.markFallbackSuccess("A", "https://a1.test")
    // written in background
    store.lastSuccessfulFallbackUrls().awaitFirst { it["A"] == "https://a1.test" }

    val restarted = PathFinder.create(store, configuration)
    restarted.updateFallbackKey("A")

    val plan = restarted.readFallbackPlan(dev.baseUrl)
    assertEquals("https://a1.test", plan.initialBaseUrl)
    assertEquals(listOf("https://dev.example.com", "https://a0.test"), plan.retryBaseUrls)
    assertEquals(1000L, plan.requestTimeout)
  }

  // flows emit on a real dispatcher, so wait in real time
  private suspend fun <T> Flow<T>.awaitFirst(predicate: (T) -> Boolean): T = withContext(Dispatchers.Default) {
    withTimeout(30_000) { first(predicate) }
  }
}

internal fun SqlDriver.count(table: String): Long = executeQuery(
  identifier = null,
  sql = "SELECT COUNT(*) FROM $table",
  mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
  parameters = 0,
).value!!

internal fun SqlDriver.queryString(sql: String): String? = executeQuery(
  identifier = null,
  sql = sql,
  mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
  parameters = 0,
).value
