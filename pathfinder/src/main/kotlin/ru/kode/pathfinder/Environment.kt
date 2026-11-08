package ru.kode.pathfinder

data class Environment(
  val id: EnvironmentId,
  val name: String,
  val baseUrl: String,
  val queryParameters: Set<String>? = null,
  /**
   * Named base urls in addition to [baseUrl]. An [UrlSpec] with [UrlSpec.baseUrlKey] uses one of them.
   */
  val additionalBaseUrls: Map<String, String> = emptyMap(),
)

@JvmInline
value class EnvironmentId(val value: String)
