package ru.kode.pathfinder

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FallbackTest {
  private val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://primary.test")
  private val configuration = Configuration(listOf(prod), emptyList(), prod.id)
  private val configA = EnvironmentFallbackConfiguration("A", listOf("https://a0.test", "https://a1.test"), 1000)
  private val default = EnvironmentFallbackConfiguration(
    DEFAULT_ENVIRONMENT_PARAMETERS_KEY,
    listOf("https://d0.test"),
    2000,
  )

  private suspend fun pathFinder() = PathFinder.create(FakeStore(), configuration).apply {
    updateFallbackEnvironments(listOf(configA, default))
  }

  @Test
  fun `plan tries primary first and then fallbacks of the key in order`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateFallbackKey("A")

    assertEquals(
      FallbackRequestPlan("https://primary.test", listOf("https://a0.test", "https://a1.test"), 1000, "A"),
      pathFinder.readFallbackPlan(prod.baseUrl),
    )
  }

  @Test
  fun `key without a configuration and an unknown key use the default one`() = runTest {
    val pathFinder = pathFinder()

    val expected = FallbackRequestPlan("https://primary.test", listOf("https://d0.test"), 2000, "Default")
    assertEquals(expected, pathFinder.readFallbackPlan(prod.baseUrl))
    pathFinder.updateFallbackKey("B")
    assertEquals(expected, pathFinder.readFallbackPlan(prod.baseUrl))
  }

  @Test
  fun `no configurations - plan has primary only and no timeout`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    pathFinder.updateFallbackKey("A")

    assertEquals(
      FallbackRequestPlan("https://primary.test", emptyList(), null, "A"),
      pathFinder.readFallbackPlan(prod.baseUrl),
    )
  }

  @Test
  fun `allowFallbacks = false - plan has primary only`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateFallbackKey("A")
    pathFinder.markFallbackSuccess("A", "https://a1.test")

    val plan = pathFinder.readFallbackPlan(prod.baseUrl, allowFallbacks = false)

    assertEquals("https://primary.test", plan.initialBaseUrl)
    assertEquals(emptyList(), plan.retryBaseUrls)
  }

  @Test
  fun `markFallbackSuccess moves the url first and keeps the order of the rest`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateFallbackKey("A")

    pathFinder.markFallbackSuccess("A", "https://a1.test")
    val plan = pathFinder.readFallbackPlan(prod.baseUrl)
    assertEquals("https://a1.test", plan.initialBaseUrl)
    assertEquals(listOf("https://primary.test", "https://a0.test"), plan.retryBaseUrls)

    // primary recovers
    pathFinder.markFallbackSuccess("A", "https://primary.test")
    assertEquals(
      listOf("https://primary.test", "https://a0.test", "https://a1.test"),
      pathFinder.readFallbackPlan(prod.baseUrl).let { listOf(it.initialBaseUrl) + it.retryBaseUrls },
    )
  }

  @Test
  fun `markFallbackSuccess is per key and ignores urls which are not in the plan`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.markFallbackSuccess("A", "https://a1.test")
    pathFinder.markFallbackSuccess("Default", "https://stale.test")

    assertEquals("https://primary.test", pathFinder.readFallbackPlan(prod.baseUrl).initialBaseUrl)
    pathFinder.updateFallbackKey("A")
    assertEquals("https://a1.test", pathFinder.readFallbackPlan(prod.baseUrl).initialBaseUrl)
  }
}
