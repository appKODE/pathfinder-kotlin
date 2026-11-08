package ru.kode.pathfinder.android.store.adapter

import app.cash.sqldelight.ColumnAdapter

/**
 * Stores a map as `key=value` lines. Keys must not contain '=' or line breaks, values must not contain line breaks.
 */
class StringMapColumnAdapter : ColumnAdapter<Map<String, String>, String> {
  override fun encode(value: Map<String, String>): String = value.entries.joinToString("\n") { (k, v) -> "$k=$v" }

  override fun decode(databaseValue: String): Map<String, String> = databaseValue
    .lineSequence()
    .filter { it.isNotEmpty() }
    .associate { it.substringBefore('=') to it.substringAfter('=') }
}
