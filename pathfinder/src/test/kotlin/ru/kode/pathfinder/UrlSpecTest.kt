package ru.kode.pathfinder

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlSpecTest {
  @Test
  fun `id is method and template`() {
    assertEquals(UrlSpecId("PATCH_users/{id}"), UrlSpec("users/{id}", UrlSpec.HttpMethod.PATCH, "User").id)
  }

  @Test
  fun `parses path variables`() {
    fun variables(template: String) = UrlSpec(template, UrlSpec.HttpMethod.GET, "").pathVariables.toList()

    assertEquals(emptyList(), variables("users/list"))
    assertEquals(listOf("categoryId", "userId"), variables("categories/{categoryId}/users/{userId}"))
    assertEquals(listOf("a", "b"), variables("{a}{b}"))
  }

  @Test
  fun `url configuration parses path variables`() {
    val configuration = UrlConfiguration(
      id = UrlConfiguration.Id("1"),
      pathTemplate = "{a}/x/{b}",
      name = "",
      httpMethod = UrlSpec.HttpMethod.GET,
    )

    assertEquals(listOf("a", "b"), configuration.pathVariables.toList())
  }
}
