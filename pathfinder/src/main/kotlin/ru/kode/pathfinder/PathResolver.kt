package ru.kode.pathfinder

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
  suspend fun buildUrl(
    id: UrlSpecId,
    pathVariables: Map<String, String> = emptyMap(),
    queryParameters: Map<String, String> = emptyMap(),
  ): String
}
