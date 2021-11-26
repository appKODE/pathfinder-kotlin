package ru.kode.pathfinder.android.ui.screen.urllist.entity

data class PathVariableEditorProps(
  val variables: List<Variable>,
) {
  data class Variable(
    val name: String,
    val value: String,
  )
}
