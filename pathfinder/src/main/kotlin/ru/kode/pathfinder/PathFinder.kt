package ru.kode.pathfinder

import java.util.UUID

data class UrlSpec(
  /**
   * Unique identifier of this url specification
   */
  val id: UUID,

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

  /**
   * A list of query parameter names which will be accessible for modification.
   */
  val queryParameterNames: List<String>,

  /**
   * Additional information about the url
   */
  val meta: Meta?
) {

  data class Meta(
    val httpMethod: HttpMethod,
    val tags: List<String>,
    val description: String,
  )

  enum class HttpMethod { GET, POST, PUT, UPDATE, DELETE }
}

data class Environment(
  val id: UUID,
  val name: String,
  val baseUrl: String,
)

interface PathFinderBuilder {
  fun build(specifications: Map<Environment, List<UrlSpec>>): PathFinder
}

interface PathFinder {
  val currentResolver: PathResolver

  fun switchEnvironment(environmentId: UUID): PathResolver

  val environments: List<Environment>
  val urlSpecs: List<UrlSpec>

  fun preConfigureUrl(
    urlId: UUID,
    pathParameters: Map<String, String>? = null,
    queryParameters: Map<String, String>? = null,
    customBaseUrl: String? = null,
    customHttpCode: Int? = null,
    customExampleName: String? = null,
  )
}

interface PathResolver {
  /**
   * Builds url for specification [id].
   *
   * Example:
   * ```
   * //
   * // for UrlSpec("categories/{categoryId}/users/{userId}", queryParameters = listOf("sort", "key"))
   * //
   * buildUrl(
   *   id = myUrlId,
   *   pathParameters = mapOf("categoryId" to "333", "userId" to "444"),
   *   queryParameters = mapOf("sort" to "desc", "key" to "none", "random" to "true")
   * )
   * ```
   *
   * will produce
   *
   * ```
   * "categories/333/users/444?sort=desc&key=none&random=true"
   * ```
   */
  fun buildUrl(
    id: UUID,
    pathParameters: Map<String, String>,
    queryParameters: Map<String, String>,
  ): String
}

//
// Use case:
//
// 1. User of DEBUG PANEL passes Map<Environment, List<UrlSpec>> as its configuration parameters
//
// 2. DEBUG PANEL internally creates PathFinder, sets default environment, orchestrates environment switching
//    by calling 'switchEnvironment'
//
// 3. DEBUG_PANEL calls PathFinder.preConfigureUrl when user changes url settings in DEBUG PANEL
//
// 3. DEBUG PANEL *hides* PathFinder, *hides* switchEnvironment()/preConfigureUrl()/etc and client
//    of debug panel only receives access to a 'currentResolver' property which it can use to call `buildUrl`
//
