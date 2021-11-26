package ru.kode.pathfinder

/**
 * A custom configuration of an [UrlSpec].
 *
 * Each [UrlSpec] can have different configurations depending on [Environment].
 */
data class UrlConfiguration(
  val id: Id,
  val pathTemplate: String,
  val name: String,
  val httpMethod: UrlSpec.HttpMethod,
  val pathVariableValues: Map<String, String> = emptyMap(),
  val queryParameterValues: Map<String, String> = emptyMap(),
) {

  @JvmInline
  value class Id(val value: String)

  /**
   * Returns a list of parameter names present in [pathTemplate]
   */
  val pathVariables get() = Regex("\\{(.+?)\\}").findAll(pathTemplate).map { it.groupValues[1] }
}
