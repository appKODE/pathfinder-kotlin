package ru.kode.pathfinder

/**
 * [EnvironmentFallbackConfiguration.key] used when the selected key (see [PathFinder.updateFallbackKey]) has no
 * configuration of its own.
 */
const val DEFAULT_ENVIRONMENT_PARAMETERS_KEY = "Default"

/**
 * Ordered base urls to try when the active environment's base url fails.
 * [key] is any app-chosen string which groups the fallbacks, e.g. the user's country code when some countries need
 * their own proxies.
 * [requestTimeout] is in milliseconds, per attempt.
 */
data class EnvironmentFallbackConfiguration(
  val key: String,
  val baseUrls: List<String>,
  val requestTimeout: Long,
)

/**
 * A plan for one request: try [initialBaseUrl] first, then [retryBaseUrls] in order.
 * [requestTimeout] is null when there is no fallback configuration for the selected key.
 * Report the base url which served the request with [PathFinder.markFallbackSuccess] passing [key].
 */
data class FallbackRequestPlan(
  val initialBaseUrl: String,
  val retryBaseUrls: List<String>,
  val requestTimeout: Long?,
  val key: String,
)
