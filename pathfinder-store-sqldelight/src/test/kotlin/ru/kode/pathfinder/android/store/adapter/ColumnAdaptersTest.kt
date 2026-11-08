package ru.kode.pathfinder.android.store.adapter

import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnAdaptersTest {
  @Test
  fun `string list round trip with a custom separator`() {
    val adapter = StringListColumnAdapter(separator = ":::")

    assertEquals("a:::b;c", adapter.encode(listOf("a", "b;c")))
    assertEquals(listOf("a", "b;c"), adapter.decode("a:::b;c"))
  }

  @Test
  fun `string list uses semicolon by default`() {
    assertEquals(listOf("a", "b"), StringListColumnAdapter().decode(StringListColumnAdapter().encode(listOf("a", "b"))))
  }

  @Test
  fun `string map round trip`() {
    val adapter = StringMapColumnAdapter()
    val map = mapOf("auth" to "https://auth.example.com?a=b", "cdn" to "")

    assertEquals("auth=https://auth.example.com?a=b\ncdn=", adapter.encode(map))
    assertEquals(map, adapter.decode(adapter.encode(map)))
  }

  @Test
  fun `string map of an empty string is empty`() {
    assertEquals(emptyMap(), StringMapColumnAdapter().decode(""))
  }
}
