package ru.kode.pathfinder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConfigurationTest {
  private val dev =
    Environment(EnvironmentId("dev"), "Dev", "https://dev.example.com", queryParameters = setOf("token", "lang"))
  private val prod = Environment(EnvironmentId("prod"), "Prod", "https://example.com")
  private val users = UrlSpec("users/{id}", UrlSpec.HttpMethod.GET, "User")
  private val login = UrlSpec("login", UrlSpec.HttpMethod.POST, "Login")
  private val configuration = Configuration(
    environments = listOf(dev, prod),
    urlSpecs = listOf(users, login),
    defaultEnvironmentId = dev.id,
  )

  @Test
  fun `checksum of a configuration without new fields equals the one of 0_11_1`() {
    // computed by Configuration.computeChecksum of 0.11.1 for the same configuration:
    // an equal checksum means stored overrides are not wiped on upgrade
    assertEquals("47760b4410d464cd01e3022c72608ea7", configuration.computeChecksum())
  }

  @Test
  fun `checksum is deterministic`() {
    assertEquals(configuration.computeChecksum(), configuration.copy().computeChecksum())
  }

  @Test
  fun `checksum changes when any field changes`() {
    val changed = listOf(
      configuration.copy(environments = listOf(dev.copy(id = EnvironmentId("dev2")), prod)),
      configuration.copy(environments = listOf(dev.copy(name = "Dev2"), prod)),
      configuration.copy(environments = listOf(dev.copy(baseUrl = "https://dev2.example.com"), prod)),
      configuration.copy(environments = listOf(dev.copy(queryParameters = setOf("token")), prod)),
      configuration.copy(environments = listOf(dev, prod.copy(additionalBaseUrls = mapOf("auth" to "https://a")))),
      configuration.copy(environments = listOf(dev)),
      configuration.copy(urlSpecs = listOf(users.copy(pathTemplate = "users/{uid}"), login)),
      configuration.copy(urlSpecs = listOf(users.copy(httpMethod = UrlSpec.HttpMethod.DELETE), login)),
      configuration.copy(urlSpecs = listOf(users.copy(name = "User2"), login)),
      configuration.copy(urlSpecs = listOf(users, login.copy(baseUrlKey = "auth"))),
      configuration.copy(urlSpecs = listOf(users)),
      configuration.copy(defaultEnvironmentId = prod.id),
    ).map { it.computeChecksum() }

    changed.forEach { assertNotEquals(configuration.computeChecksum(), it) }
    assertEquals(changed.size, changed.toSet().size)
  }

  @Test
  fun `additionalBaseUrls order does not change the checksum`() {
    val a = prod.copy(additionalBaseUrls = linkedMapOf("auth" to "https://a", "cdn" to "https://c"))
    val b = prod.copy(additionalBaseUrls = linkedMapOf("cdn" to "https://c", "auth" to "https://a"))

    assertEquals(
      configuration.copy(environments = listOf(dev, a)).computeChecksum(),
      configuration.copy(environments = listOf(dev, b)).computeChecksum(),
    )
  }

  @Test
  fun `checksum with additionalBaseUrls equals the one of 0_12_0`() {
    // computed by Configuration.computeChecksum of 0.12.0 (JVM-only, java.util.TreeMap) for the same configuration
    val prodWithUrls = prod.copy(additionalBaseUrls = linkedMapOf("cdn" to "https://c", "auth" to "https://a"))
    val usersWithKey = users.copy(baseUrlKey = "auth")

    assertEquals(
      "304f65a59cabddce27834b9e48ac26f1",
      Configuration(listOf(dev, prodWithUrls), listOf(usersWithKey, login), dev.id).computeChecksum(),
    )
  }
}
