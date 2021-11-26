package ru.kode.pathfinder

data class UrlSpec(
  /**
   * A path template relative to baseUrl.
   *
   * Can contain variables which must be surrounded by curly braces.
   *
   * Examples:
   * ```
   * "users/list"
   * "categories/{categoryId}/users/{userId}"
   * ```
   */
  val pathTemplate: String,

  val httpMethod: HttpMethod,
  val name: String,
) {

  /**
   * Unique identifier of this url specification
   */
  val id: UrlSpecId get() = UrlSpecId("${httpMethod.name}_$pathTemplate")

  enum class HttpMethod { GET, POST, PUT, UPDATE, PATCH, DELETE }

  /**
   * Returns a list of parameter names present in [pathTemplate]
   */
  val pathVariables = Regex("\\{(.+?)\\}").findAll(pathTemplate).map { it.groupValues[1] }
}

@JvmInline
value class UrlSpecId(val value: String)
