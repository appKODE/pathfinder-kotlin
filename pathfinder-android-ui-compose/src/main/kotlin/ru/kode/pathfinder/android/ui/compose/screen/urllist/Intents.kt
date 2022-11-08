package ru.kode.pathfinder.android.ui.compose.screen.urllist

import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

class Intents : ViewIntents() {
  val changeEnvironment = intent(name = "changeEnvironment")
  val dismissActiveEnvSelector = intent(name = "dismissActiveEnvSelector")
  val setActiveEnvironment = intent<EnvironmentId>(name = "setActiveEnvironment")
  val editPathVariables = intent<UrlConfiguration.Id>(name = "editPathVariables")
  val dismissEditPathVariablesDialog = intent(name = "dismissEditPathVariablesDialog")
  val saveEditedPathVariables = intent<List<PathVariableEditorProps.Variable>>(name = "saveEditedPathVariables")
  val editQueryParameters = intent<UrlConfiguration.Id>(name = "editQueryParameters")
  val dismissEditQueryParametersDialog = intent(name = "dismissEditQueryParametersDialog")
  val saveEditedQueryParameters = intent<List<QueryParameterEditorProps.Parameter>>(name = "saveEditedQueryParameters")
}
