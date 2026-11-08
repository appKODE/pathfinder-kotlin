package ru.kode.pathfinder

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultResolverTest {
  private val dev = Environment(
    id = EnvironmentId("dev"),
    name = "Dev",
    baseUrl = "https://dev.example.com",
    queryParameters = setOf("lang"),
    additionalBaseUrls = mapOf("auth" to "https://auth.dev.example.com"),
  )
  private val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://example.com")
  private val list = UrlSpec(pathTemplate = "users", httpMethod = UrlSpec.HttpMethod.GET, name = "Users")
  private val user = UrlSpec(
    pathTemplate = "categories/{categoryId}/users/{userId}",
    httpMethod = UrlSpec.HttpMethod.GET,
    name = "User",
  )
  private val login = UrlSpec(
    pathTemplate = "login",
    httpMethod = UrlSpec.HttpMethod.POST,
    name = "Login",
    baseUrlKey = "auth",
  )
  private val configuration = Configuration(
    environments = listOf(dev, prod),
    urlSpecs = listOf(list, user, login),
    defaultEnvironmentId = dev.id,
  )

  private suspend fun resolver(environmentId: EnvironmentId = dev.id): Pair<FakeStore, DefaultResolver> {
    val store = FakeStore()
    store.saveConfiguration(configuration)
    return store to DefaultResolver(store, environmentId)
  }

  @Test
  fun `substitutes path variables`() = runTest {
    val (_, resolver) = resolver()

    assertEquals(
      "https://dev.example.com/categories/3/users/4",
      resolver.buildUrl(user.id, pathVariables = mapOf("categoryId" to "3", "userId" to "4")),
    )
  }

  @Test
  fun `fails listing missing path variables`() = runTest {
    val (_, resolver) = resolver()

    val error = assertFailsWith<IllegalStateException> {
      resolver.buildUrl(user.id, pathVariables = mapOf("categoryId" to "3"))
    }
    assertContains(error.message.orEmpty(), "\"userId\"")
    assertFailsWith<IllegalStateException> { resolver.buildUrl(user.id) }
      .also { assertContains(it.message.orEmpty(), "categoryId, userId") }
  }

  @Test
  fun `stored path variables override call arguments, blank stored values are ignored`() = runTest {
    val (store, resolver) = resolver()
    val configurationId = store.readUrlConfiguration(user.id, dev.id)!!.id

    store.updatePathVariables(configurationId, mapOf("categoryId" to "stored", "userId" to " "))

    assertEquals(
      "https://dev.example.com/categories/stored/users/4",
      resolver.buildUrl(user.id, pathVariables = mapOf("categoryId" to "3", "userId" to "4")),
    )
  }

  @Test
  fun `stored query parameters override call arguments, blank stored values are ignored`() = runTest {
    val (store, resolver) = resolver()
    val configurationId = store.readUrlConfiguration(list.id, dev.id)!!.id

    assertEquals(
      "https://dev.example.com/users?lang=en",
      resolver.buildUrl(list.id, queryParameters = mapOf("lang" to "en"))
    )

    store.updateQueryParameters(configurationId, mapOf("lang" to "ru"))
    assertEquals(
      "https://dev.example.com/users?lang=ru",
      resolver.buildUrl(list.id, queryParameters = mapOf("lang" to "en"))
    )

    store.updateQueryParameters(configurationId, mapOf("lang" to ""))
    assertEquals(
      "https://dev.example.com/users?lang=en",
      resolver.buildUrl(list.id, queryParameters = mapOf("lang" to "en"))
    )
  }

  @Test
  fun `keeps query parameter order and drops blank values`() = runTest {
    val (_, resolver) = resolver()

    assertEquals(
      "https://dev.example.com/users?sort=desc&page=2",
      resolver.buildUrl(list.id, queryParameters = mapOf("sort" to "desc", "empty" to " ", "page" to "2")),
    )
  }

  @Test
  fun `no query parameters produce no question mark`() = runTest {
    val (_, resolver) = resolver()

    assertEquals("https://dev.example.com/users", resolver.buildUrl(list.id))
    assertEquals("https://dev.example.com/users", resolver.buildUrl(list.id, queryParameters = mapOf("lang" to "")))
  }

  @Test
  fun `fails on unknown spec or environment`() = runTest {
    val (_, resolver) = resolver()

    assertFailsWith<IllegalStateException> { resolver.buildUrl(UrlSpecId("GET_unknown")) }
    assertFailsWith<IllegalStateException> { resolver(EnvironmentId("unknown")).second.buildUrl(list.id) }
  }

  @Test
  fun `baseUrlKey uses the additional base url`() = runTest {
    val (_, resolver) = resolver()

    assertEquals("https://auth.dev.example.com/login", resolver.buildUrl(login.id))
  }

  @Test
  fun `missing baseUrlKey fails naming key and environment`() = runTest {
    val (_, resolver) = resolver(prod.id)

    val error = assertFailsWith<IllegalStateException> { resolver.buildUrl(login.id) }
    assertEquals("no base url 'auth' in environment prod", error.message)
  }
}
