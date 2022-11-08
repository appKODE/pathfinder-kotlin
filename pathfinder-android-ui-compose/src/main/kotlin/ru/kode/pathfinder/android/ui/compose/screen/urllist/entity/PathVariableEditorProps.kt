package ru.kode.pathfinder.android.ui.compose.screen.urllist.entity

data class PathVariableEditorProps(
  val variables: List<Variable>,
) {
  data class Variable(
    val name: String,
    val value: String,
  )
}
