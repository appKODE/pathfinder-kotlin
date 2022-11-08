package ru.kode.pathfinder.android.ui.compose.screen.urllist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.android.ui.compose.R
import ru.kode.pathfinder.android.ui.compose.base.BaseScreenUi
import ru.kode.pathfinder.android.ui.compose.screen.urllist.component.PathVariableEditor
import ru.kode.pathfinder.android.ui.compose.screen.urllist.component.UrlListItem
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

internal class UrlListUi : BaseScreenUi<ViewState, Intents>(Intents()) {
  @Composable
  override fun Content(viewState: ViewState) {
    val content = viewState.content ?: return
    ModalBottomSheetScaffold(contentState = content) {
      Column(
        modifier = Modifier.navigationBarsPadding().imePadding(),
      ) {
        Spacer(modifier = Modifier.height(16.dp))
        ActiveEnvironmentHeader(content)
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        UrlList(content.urls)
        if (content.showPathVariableEditor != null) {
          PathVariableEditorDialog(props = content.showPathVariableEditor)
        }
        if (content.showQueryParameterEditor != null) {
          QueryParameterEditorDialog(props = content.showQueryParameterEditor)
        }
      }
    }
  }

  @Composable
  private fun PathVariableEditorDialog(props: PathVariableEditorProps) {
    val editorState = remember { props.variables.toMutableStateList() }
    Dialog(onDismissRequest = intents.dismissEditPathVariablesDialog) {
      PathVariableEditor(
        props = props.copy(variables = editorState),
        onValueChange = { index, value -> editorState[index] = editorState[index].copy(value = value) },
        onConfirm = { intents.saveEditedPathVariables(editorState); intents.dismissEditPathVariablesDialog() },
        onDismiss = intents.dismissEditPathVariablesDialog,
      )
    }
  }

  @Composable
  private fun QueryParameterEditorDialog(props: QueryParameterEditorProps) {
    val editorState = remember { props.parameters.toMutableStateList() }
    Dialog(onDismissRequest = intents.dismissEditQueryParametersDialog) {
      PathVariableEditor(
        props = props.copy(parameters = editorState),
        onValueChange = { index, value -> editorState[index] = editorState[index].copy(value = value) },
        onConfirm = { intents.saveEditedQueryParameters(editorState); intents.dismissEditQueryParametersDialog() },
        onDismiss = intents.dismissEditQueryParametersDialog,
      )
    }
  }

  @Composable
  private fun ActiveEnvironmentHeader(state: ViewState.Content) {
    Column(
      modifier = Modifier
    ) {
      Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        style = MaterialTheme.typography.subtitle1,
        text = stringResource(id = R.string.url_list_current_env),
      )
      Row(
        modifier = Modifier
          .padding(start = 24.dp, end = 24.dp)
          .fillMaxWidth(),
        verticalAlignment = CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.h4,
          text = state.activeEnvironment.name,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = intents.changeEnvironment) {
          Text(
            modifier = Modifier,
            style = MaterialTheme.typography.button,
            text = stringResource(id = R.string.url_list_current_env_change),
          )
        }
      }
    }
  }

  @Composable
  private fun UrlList(urls: List<UrlConfiguration>) {
    val expandedIds = remember { mutableStateListOf<UrlConfiguration.Id>() }
    LazyColumn {
      itemsIndexed(urls, key = { _, item -> item.id.value }) { index, item ->
        if (index != 0) {
          Divider()
        }
        UrlListItem(
          props = item,
          isExpanded = expandedIds.contains(item.id),
          onClick = {
            if (expandedIds.contains(item.id)) {
              expandedIds.remove(item.id)
            } else {
              expandedIds.add(item.id)
            }
          },
          onEditPathVariablesClick = {
            intents.editPathVariables(item.id)
          },
          onEditQueryParametersClick = {
            intents.editQueryParameters(item.id)
          }
        )
      }
    }
  }

  @ExperimentalMaterialApi
  @Composable
  private fun ModalBottomSheetScaffold(
    contentState: ViewState.Content,
    content: @Composable () -> Unit,
  ) {
    val sheetState = rememberModalBottomSheetState(
      initialValue = ModalBottomSheetValue.Hidden,
      confirmStateChange = { sheetValue ->
        if (sheetValue == ModalBottomSheetValue.Hidden) {
          intents.dismissActiveEnvSelector()
        }
        false
      }
    )
    LaunchedEffect(contentState.showActiveEnvSelector) {
      if (contentState.showActiveEnvSelector != null) {
        sheetState.animateTo(ModalBottomSheetValue.Expanded)
      } else {
        sheetState.hide()
      }
    }
    ModalBottomSheetLayout(
      sheetState = sheetState,
      sheetContent = { ModalSheetContent(contentState) },
      content = content
    )
  }

  @Composable
  private fun ModalSheetContent(state: ViewState.Content) {
    if (state.showActiveEnvSelector != null) {
      ActiveEnvSelector(state.showActiveEnvSelector, selectedEnvironmentId = state.activeEnvironment.id)
    } else {
      Spacer(modifier = Modifier.height(48.dp))
    }
  }

  @Composable
  private fun ActiveEnvSelector(environments: List<Environment>, selectedEnvironmentId: EnvironmentId) {
    Column(
      modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
      Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        style = MaterialTheme.typography.h6,
        text = stringResource(id = R.string.select_environment_title),
      )
      environments.forEach { item ->
        Row(
          modifier = Modifier
            .clickable(onClick = { intents.setActiveEnvironment(item.id) })
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
          verticalAlignment = CenterVertically
        ) {
          RadioButton(selected = item.id == selectedEnvironmentId, onClick = null)
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            style = MaterialTheme.typography.body1,
            text = item.name,
          )
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
