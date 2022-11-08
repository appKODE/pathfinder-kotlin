package ru.kode.pathfinder.android.ui.compose.screen.urllist

import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

data class ViewState(
  val content: Content? = null
) {
  data class Content(
    val activeEnvironment: Environment,
    val urls: List<UrlConfiguration>,
    val editedId: UrlConfiguration.Id? = null,
    val showActiveEnvSelector: List<Environment>? = null,
    val showPathVariableEditor: PathVariableEditorProps? = null,
    val showQueryParameterEditor: QueryParameterEditorProps? = null,
  )
}
