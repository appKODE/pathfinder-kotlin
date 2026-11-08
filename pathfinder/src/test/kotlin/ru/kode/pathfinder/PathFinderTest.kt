package ru.kode.pathfinder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PathFinderTest {
  private val dev = Environment(
    id = EnvironmentId("dev"),
    name = "Dev",
    baseUrl = "https://dev.example.com",
    additionalBaseUrls = mapOf("auth" to "https://auth.dev.example.com"),
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

  @Test
  fun `buildUrl works right after create`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    assertEquals("https://dev.example.com/users/1", pathFinder.buildUrl(users.id, mapOf("id" to "1")))
  }

  @Test
  fun `updateEnvironment and switchEnvironment change url, current environment and notify listeners`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    val switches = CopyOnWriteArrayList<EnvironmentId>()
    pathFinder.addListener(
      object : PathFinder.Listener {
        override fun onEnvironmentSwitch(environmentId: EnvironmentId) {
          switches.add(environmentId)
        }
      },
    )

    pathFinder.updateEnvironment(prod.id) { it.copy(baseUrl = "https://dynamic.example.com") }
    pathFinder.switchEnvironment(prod.id)

    assertEquals("https://dynamic.example.com/users/1", pathFinder.buildUrl(users.id, mapOf("id" to "1")))
    assertEquals("https://dynamic.example.com", pathFinder.currentEnvironment.baseUrl)
    assertEquals(listOf(prod.id), switches.toList())

    pathFinder.updateEnvironment(prod.id) { it.copy(baseUrl = "https://other.example.com") }

    assertEquals("https://other.example.com", pathFinder.currentEnvironment.baseUrl)
    // a change of the active environment notifies too
    assertEquals(listOf(prod.id, prod.id), switches.toList())
  }

  @Test
  fun `baseUrlKey resolves additional base url and fails on missing key`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    assertEquals("https://auth.dev.example.com/login", pathFinder.buildUrl(login.id))

    pathFinder.switchEnvironment(prod.id)
    assertFailsWith<IllegalStateException> { pathFinder.buildUrl(login.id) }
  }

  @Test
  fun `buildUrl with environmentId uses given environment`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    assertEquals("https://example.com/users/1", pathFinder.buildUrl(prod.id, users.id, mapOf("id" to "1")))
    assertEquals(dev.id, pathFinder.currentEnvironment.id)
  }
}

private class FakeStore : Store {
  private val environments = MutableStateFlow<List<Environment>>(emptyList())
  private val activeId = MutableStateFlow<EnvironmentId?>(null)
  private var specs = emptyList<UrlSpec>()

  override suspend fun saveConfiguration(configuration: Configuration) {
    environments.value = configuration.environments
    specs = configuration.urlSpecs
    if (configuration.environments.none { it.id == activeId.value }) activeId.value = configuration.defaultEnvironmentId
  }

  override suspend fun replaceEnvironment(environment: Environment) {
    environments.value = environments.value.filter { it.id != environment.id } + environment
  }

  override suspend fun changeActiveEnvironment(id: EnvironmentId) {
    activeId.value = id
  }

  override fun activeEnvironmentId(): Flow<EnvironmentId?> = activeId
  override suspend fun readActiveEnvironmentId(): EnvironmentId? = activeId.value
  override fun environments(): Flow<List<Environment>> = environments
  override suspend fun readEnvironmentById(id: EnvironmentId) = environments.value.find { it.id == id }
  override suspend fun readEnvironments() = environments.value

  override fun urlConfigurations(environmentId: EnvironmentId): Flow<List<UrlConfiguration>> =
    environments.map { specs.map { it.toConfiguration(environmentId) } }

  override suspend fun readUrlConfiguration(urlSpecId: UrlSpecId, environmentId: EnvironmentId) =
    specs.find { it.id == urlSpecId }?.toConfiguration(environmentId)

  override fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>) = Unit
  override fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>) = Unit

  private fun UrlSpec.toConfiguration(environmentId: EnvironmentId) = UrlConfiguration(
    id = UrlConfiguration.Id("${environmentId.value}_${id.value}"),
    pathTemplate = pathTemplate,
    name = name,
    httpMethod = httpMethod,
    baseUrlKey = baseUrlKey,
  )
}
