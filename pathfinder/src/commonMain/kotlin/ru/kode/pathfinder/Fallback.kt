package ru.kode.pathfinder

/**
 * Region code of the [EnvironmentFallbackConfiguration] used for a region without a configuration of its own.
 */
const val DEFAULT_ENVIRONMENT_PARAMETERS_KEY = "Default"

/**
 * Ordered base urls to try when the active environment's base url fails in region [regionCode].
 * [requestTimeout] is in milliseconds, per attempt.
 */
data class EnvironmentFallbackConfiguration(
  val regionCode: String,
  val baseUrls: List<String>,
  val requestTimeout: Long,
)

/**
 * A plan for one request: try [initialBaseUrl] first, then [retryBaseUrls] in order.
 * [requestTimeout] is null when there is no fallback configuration for the region.
 * Report the base url which served the request with [PathFinder.markFallbackSuccess] passing [regionCode].
 */
data class FallbackRequestPlan(
  val initialBaseUrl: String,
  val retryBaseUrls: List<String>,
  val requestTimeout: Long?,
  val regionCode: String,
)
