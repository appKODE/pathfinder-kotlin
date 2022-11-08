package ru.kode.pathfinder.android.ui.compose.screen.urllist.entity

data class QueryParameterEditorProps(
  val parameters: List<Parameter>,
) {
  data class Parameter(
    val name: String,
    val value: String,
  )
}
