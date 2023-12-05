package ru.kode.pathfinder.android.store.adapter

import app.cash.sqldelight.ColumnAdapter

class StringListColumnAdapter(private val separator: String = ";") :
  ColumnAdapter<List<String>, String> {
  override fun encode(value: List<String>): String {
    return value.joinToString(separator = separator)
  }

  override fun decode(databaseValue: String): List<String> {
    return databaseValue.split(separator)
  }
}
