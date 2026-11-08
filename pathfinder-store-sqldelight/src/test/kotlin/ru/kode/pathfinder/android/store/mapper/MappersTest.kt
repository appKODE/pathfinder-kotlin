package ru.kode.pathfinder.android.store.mapper

import ru.kode.pathfinder.UrlSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MappersTest {
  @Test
  fun `http method round trips through its stored code`() {
    UrlSpec.HttpMethod.entries.forEach { method ->
      assertEquals(method, method.toStorageModel().toDomainModel())
    }
    assertEquals("UPDATE", UrlSpec.HttpMethod.UPDATE.toStorageModel())
  }

  @Test
  fun `unknown http method code fails`() {
    assertFailsWith<IllegalStateException> { "HEAD".toDomainModel() }
  }
}
