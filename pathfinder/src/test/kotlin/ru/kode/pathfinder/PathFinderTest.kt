package ru.kode.pathfinder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

  @Test
  fun `create keeps the stored active environment if it is still configured`() = runTest {
    val store = FakeStore()
    PathFinder.create(store, configuration).switchEnvironment(prod.id)

    assertEquals(prod.id, PathFinder.create(store, configuration).currentEnvironment.id)
  }

  @Test
  fun `create falls back to the default environment if the stored one is gone`() = runTest {
    val store = FakeStore()
    PathFinder.create(store, configuration).switchEnvironment(prod.id)

    val pathFinder = PathFinder.create(store, configuration.copy(environments = listOf(dev)))

    assertEquals(dev.id, pathFinder.currentEnvironment.id)
  }

  @Test
  fun `switchEnvironment to an unknown id fails and keeps the state`() = runTest {
    val store = FakeStore()
    val pathFinder = PathFinder.create(store, configuration)

    assertFailsWith<IllegalStateException> { pathFinder.switchEnvironment(EnvironmentId("unknown")) }

    assertEquals(dev, pathFinder.currentEnvironment)
    assertEquals(dev.id, store.readActiveEnvironmentId())
  }

  @Test
  fun `replaceEnvironment with a new id adds an environment`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    val stage = Environment(id = EnvironmentId("stage"), name = "Stage", baseUrl = "https://stage.example.com")

    pathFinder.replaceEnvironment(stage)

    assertEquals("https://stage.example.com/users/1", pathFinder.buildUrl(stage.id, users.id, mapOf("id" to "1")))
    assertEquals(listOf(dev, prod, stage), pathFinder.store.readEnvironments())
    assertEquals(dev, pathFinder.currentEnvironment)
  }

  @Test
  fun `replaceEnvironment of the active environment updates the current one`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    pathFinder.replaceEnvironment(dev.copy(name = "Renamed"))

    assertEquals("Renamed", pathFinder.currentEnvironment.name)
  }

  @Test
  fun `updateEnvironment of an unknown id fails`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    assertFailsWith<IllegalStateException> { pathFinder.updateEnvironment(EnvironmentId("unknown")) { it } }
  }

  @Test
  fun `updateEachEnvironment applies to all environments`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    pathFinder.updateEachEnvironment { it.copy(baseUrl = it.baseUrl.replace("https", "http")) }

    assertEquals(
      listOf("http://dev.example.com", "http://example.com"),
      pathFinder.store.readEnvironments().map { it.baseUrl },
    )
    assertEquals("http://dev.example.com", pathFinder.currentEnvironment.baseUrl)
  }

  @Test
  fun `changing the id in an updater fails`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    assertFailsWith<IllegalStateException> { pathFinder.updateEnvironment(dev.id) { it.copy(id = EnvironmentId("x")) } }
    assertFailsWith<IllegalStateException> { pathFinder.updateEachEnvironment { it.copy(id = EnvironmentId("x")) } }
    assertEquals(listOf(dev, prod), pathFinder.store.readEnvironments())
  }

  @Test
  fun `saveConfiguration at runtime replaces environments and refreshes the current one`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    val stage = Environment(id = EnvironmentId("stage"), name = "Stage", baseUrl = "https://stage.example.com")

    pathFinder.saveConfiguration(configuration.copy(environments = listOf(stage), defaultEnvironmentId = stage.id))

    assertEquals(stage, pathFinder.currentEnvironment)
    assertEquals("https://stage.example.com/users/1", pathFinder.buildUrl(users.id, mapOf("id" to "1")))
  }

  @Test
  fun `follows external store changes and notifies listeners`() = runTest {
    val store = FakeStore()
    val pathFinder = PathFinder.create(store, configuration)
    val switches = pathFinder.recordSwitches()

    // the debug panel changes the store directly
    store.changeActiveEnvironment(prod.id)
    awaitUntil { pathFinder.currentEnvironment.id == prod.id && switches.isNotEmpty() }
    assertEquals(listOf(prod.id), switches.toList())

    store.replaceEnvironment(prod.copy(baseUrl = "https://external.example.com"))
    awaitUntil { pathFinder.currentEnvironment.baseUrl == "https://external.example.com" && switches.size == 2 }
    assertEquals(listOf(prod.id, prod.id), switches.toList())
  }

  @Test
  fun `no notification for an inactive environment change, none after removeListener`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    val switches = CopyOnWriteArrayList<EnvironmentId>()
    val listener = object : PathFinder.Listener {
      override fun onEnvironmentSwitch(environmentId: EnvironmentId) {
        switches.add(environmentId)
      }
    }
    pathFinder.addListener(listener)

    pathFinder.updateEnvironment(prod.id) { it.copy(name = "Prod2") }
    assertTrue(switches.isEmpty())

    pathFinder.removeListener(listener)
    pathFinder.switchEnvironment(prod.id)
    assertTrue(switches.isEmpty())
    assertEquals(prod.id, pathFinder.currentEnvironment.id)
  }

  @Test
  fun `concurrent updates are not lost`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)

    withContext(Dispatchers.Default) {
      (1..50).map { async { pathFinder.updateEnvironment(dev.id) { it.copy(name = it.name + "x") } } }.awaitAll()
    }

    assertEquals("Dev" + "x".repeat(50), pathFinder.store.readEnvironmentById(dev.id)!!.name)
    assertEquals("Dev" + "x".repeat(50), pathFinder.currentEnvironment.name)
  }

  private fun PathFinder.recordSwitches(): List<EnvironmentId> {
    val switches = CopyOnWriteArrayList<EnvironmentId>()
    addListener(
      object : PathFinder.Listener {
        override fun onEnvironmentSwitch(environmentId: EnvironmentId) {
          switches.add(environmentId)
        }
      },
    )
    return switches
  }

  // the store observer runs on Dispatchers.IO, so wait in real time
  private suspend fun awaitUntil(condition: () -> Boolean) = withContext(Dispatchers.Default) {
    withTimeout(5_000) {
      while (!condition()) delay(10)
    }
  }
}
