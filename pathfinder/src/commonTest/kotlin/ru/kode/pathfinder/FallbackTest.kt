package ru.kode.pathfinder

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FallbackTest {
  private val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://primary.test")
  private val configuration = Configuration(listOf(prod), emptyList(), prod.id)
  private val ru = EnvironmentFallbackConfiguration("RU", listOf("https://ru0.test", "https://ru1.test"), 1000)
  private val default = EnvironmentFallbackConfiguration(
    DEFAULT_ENVIRONMENT_PARAMETERS_KEY,
    listOf("https://d0.test"),
    2000,
  )

  private suspend fun pathFinder() = PathFinder.create(FakeStore(), configuration).apply {
    updateFallbackEnvironments(listOf(ru, default))
  }

  @Test
  fun `plan tries primary first and then fallbacks of the region in order`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateApiFallbackRegionCode("RU")

    assertEquals(
      FallbackRequestPlan("https://primary.test", listOf("https://ru0.test", "https://ru1.test"), 1000, "RU"),
      pathFinder.readFallbackPlan(prod.baseUrl),
    )
  }

  @Test
  fun `region without a configuration and an unknown region use the default one`() = runTest {
    val pathFinder = pathFinder()

    val expected = FallbackRequestPlan("https://primary.test", listOf("https://d0.test"), 2000, "Default")
    assertEquals(expected, pathFinder.readFallbackPlan(prod.baseUrl))
    pathFinder.updateApiFallbackRegionCode("FR")
    assertEquals(expected, pathFinder.readFallbackPlan(prod.baseUrl))
  }

  @Test
  fun `no configurations - plan has primary only and no timeout`() = runTest {
    val pathFinder = PathFinder.create(FakeStore(), configuration)
    pathFinder.updateApiFallbackRegionCode("RU")

    assertEquals(
      FallbackRequestPlan("https://primary.test", emptyList(), null, "RU"),
      pathFinder.readFallbackPlan(prod.baseUrl),
    )
  }

  @Test
  fun `allowFallbacks = false - plan has primary only`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateApiFallbackRegionCode("RU")
    pathFinder.markFallbackSuccess("RU", "https://ru1.test")

    val plan = pathFinder.readFallbackPlan(prod.baseUrl, allowFallbacks = false)

    assertEquals("https://primary.test", plan.initialBaseUrl)
    assertEquals(emptyList(), plan.retryBaseUrls)
  }

  @Test
  fun `markFallbackSuccess moves the url first and keeps the order of the rest`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.updateApiFallbackRegionCode("RU")

    pathFinder.markFallbackSuccess("RU", "https://ru1.test")
    val plan = pathFinder.readFallbackPlan(prod.baseUrl)
    assertEquals("https://ru1.test", plan.initialBaseUrl)
    assertEquals(listOf("https://primary.test", "https://ru0.test"), plan.retryBaseUrls)

    // primary recovers
    pathFinder.markFallbackSuccess("RU", "https://primary.test")
    assertEquals(
      listOf("https://primary.test", "https://ru0.test", "https://ru1.test"),
      pathFinder.readFallbackPlan(prod.baseUrl).let { listOf(it.initialBaseUrl) + it.retryBaseUrls },
    )
  }

  @Test
  fun `markFallbackSuccess is per region and ignores urls which are not in the plan`() = runTest {
    val pathFinder = pathFinder()
    pathFinder.markFallbackSuccess("RU", "https://ru1.test")
    pathFinder.markFallbackSuccess("Default", "https://stale.test")

    assertEquals("https://primary.test", pathFinder.readFallbackPlan(prod.baseUrl).initialBaseUrl)
    pathFinder.updateApiFallbackRegionCode("RU")
    assertEquals("https://ru1.test", pathFinder.readFallbackPlan(prod.baseUrl).initialBaseUrl)
  }
}
