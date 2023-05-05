package ru.kode.pathfinder.android.ui.compose.screen.urllist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.pathfinder.android.ui.compose.R
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

@Composable
internal fun QueryParameterEditor(
  modifier: Modifier = Modifier,
  props: QueryParameterEditorProps,
  onValueChange: (index: Int, value: String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.White)
      .padding(24.dp),
  ) {
    Text(
      text = stringResource(id = R.string.edit_query_parameters_dialog_title),
      style = MaterialTheme.typography.h6
    )
    Spacer(modifier = Modifier.height(24.dp))
    props.parameters.forEachIndexed { index, parameter ->
      Spacer(modifier = Modifier.height(16.dp))
      Parameter(item = parameter, onValueChange = { onValueChange(index, it) })
    }
    Spacer(modifier = Modifier.height(24.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(id = R.string.action_cancel))
      }
      Spacer(modifier = Modifier.width(16.dp))
      TextButton(onClick = onConfirm) {
        Text(text = stringResource(id = R.string.action_save))
      }
    }
  }
}

@Composable
private fun Parameter(
  item: QueryParameterEditorProps.Parameter,
  onValueChange: (String) -> Unit,
) {
  TextField(
    modifier = Modifier.fillMaxWidth(),
    value = item.value,
    maxLines = 1,
    onValueChange = onValueChange,
    label = {
      Text(text = item.name)
    }
  )
}
