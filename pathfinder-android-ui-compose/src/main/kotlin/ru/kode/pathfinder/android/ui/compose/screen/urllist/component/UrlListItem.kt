package ru.kode.pathfinder.android.ui.compose.screen.urllist.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.android.ui.compose.R

@Composable
internal fun UrlListItem(
  modifier: Modifier = Modifier,
  props: UrlConfiguration,
  onClick: () -> Unit,
  onEditPathVariablesClick: () -> Unit,
  onEditQueryParametersClick: () -> Unit,
  isExpanded: Boolean,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .clickable(onClick = onClick)
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      MethodTag(name = props.httpMethod.name)
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          style = MaterialTheme.typography.subtitle1,
          fontWeight = FontWeight.Bold,
          text = props.pathTemplate,
        )
        Text(
          style = MaterialTheme.typography.caption,
          color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
          text = props.name,
        )
      }
    }
    if (isExpanded) {
      Details(
        pathVariables = props.pathVariableValues,
        queryParameters = props.queryParameterValues,
        onEditPathVariablesClick = onEditPathVariablesClick,
        onEditQueryParametersClick = onEditQueryParametersClick,
      )
    }
  }
}

@Composable
private fun Details(
  pathVariables: Map<String, String>,
  queryParameters: Map<String, String>,
  onEditPathVariablesClick: () -> Unit,
  onEditQueryParametersClick: () -> Unit,
) {
  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    if (pathVariables.isNotEmpty()) {
      Text(
        modifier = Modifier,
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        text = stringResource(id = R.string.url_path_variables),
      )
      Column {
        if (pathVariables.all { it.value.isEmpty() }) {
          Text(
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body1,
            text = stringResource(id = R.string.no_values),
          )
        } else {
          Text(
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body1,
            text = pathVariables
              .filterValues { it.isNotEmpty() }
              .entries.joinToString() { (key, value) -> "$key=$value" }
          )
        }
        Button(
          onClick = onEditPathVariablesClick,
          colors = ButtonDefaults.textButtonColors(),
          elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
        ) {
          Text(text = stringResource(id = R.string.action_edit))
        }
      }
    }
    if (queryParameters.isNotEmpty()) {
      Text(
        modifier = Modifier,
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        text = stringResource(id = R.string.url_query_parameters),
      )
      Column {
        if (queryParameters.all { it.value.isEmpty() }) {
          Text(
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body1,
            text = stringResource(id = R.string.no_values),
          )
        } else {
          Text(
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body1,
            text = queryParameters
              .filterValues { it.isNotEmpty() }
              .entries.joinToString() { (key, value) -> "$key=$value" }
          )
        }
        Button(
          onClick = onEditQueryParametersClick,
          colors = ButtonDefaults.textButtonColors(),
          elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
        ) {
          Text(text = stringResource(id = R.string.action_edit))
        }
      }
    }
  }
}

@Composable
private fun MethodTag(modifier: Modifier = Modifier, name: String) {
  Text(
    modifier = modifier
      .border(width = 1.dp, color = MaterialTheme.colors.secondary)
      .padding(4.dp),
    color = MaterialTheme.colors.secondary,
    style = MaterialTheme.typography.caption,
    text = name
  )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
internal fun UrlListItemPreview() {
  MaterialTheme {
    Column {
      UrlListItem(
        props = UrlConfiguration(
          id = UrlConfiguration.Id(value = "1"),
          pathTemplate = "categories/{categoryId}/users/{userId}",
          name = "Get user by id",
          httpMethod = UrlSpec.HttpMethod.GET,
          pathVariableValues = emptyMap(),
          queryParameterValues = emptyMap(),
        ),
        isExpanded = false,
        onClick = {},
        onEditPathVariablesClick = {},
        onEditQueryParametersClick = {},
      )
      Divider()
      UrlListItem(
        props = UrlConfiguration(
          id = UrlConfiguration.Id(value = "2"),
          pathTemplate = "/{branchId}/categories/{categoryId}",
          name = "Get category by id",
          httpMethod = UrlSpec.HttpMethod.GET,
          pathVariableValues = mapOf("branchId" to "mid-west", "categoryId" to "category1"),
          queryParameterValues = emptyMap(),
        ),
        isExpanded = false,
        onClick = {},
        onEditPathVariablesClick = {},
        onEditQueryParametersClick = {},
      )
      Divider()
      UrlListItem(
        props = UrlConfiguration(
          id = UrlConfiguration.Id(value = "3"),
          pathTemplate = "/{branchId}/categories/{categoryId}",
          name = "Get category by id",
          httpMethod = UrlSpec.HttpMethod.POST,
          pathVariableValues = mapOf("branchId" to "mid-west", "categoryId" to "category1"),
          queryParameterValues = mapOf("sortDirection" to "ascending", "reverseOrder" to "true"),
        ),
        isExpanded = false,
        onClick = {},
        onEditPathVariablesClick = {},
        onEditQueryParametersClick = {},
      )
      Divider()
      UrlListItem(
        props = UrlConfiguration(
          id = UrlConfiguration.Id(value = "4"),
          pathTemplate = "/categories/default",
          name = "Get category by id",
          httpMethod = UrlSpec.HttpMethod.GET,
          pathVariableValues = emptyMap(),
          queryParameterValues = mapOf("sortDirection" to "ascending", "reverseOrder" to "true"),
        ),
        isExpanded = false,
        onClick = {},
        onEditPathVariablesClick = {},
        onEditQueryParametersClick = {},
      )
    }
  }
}
