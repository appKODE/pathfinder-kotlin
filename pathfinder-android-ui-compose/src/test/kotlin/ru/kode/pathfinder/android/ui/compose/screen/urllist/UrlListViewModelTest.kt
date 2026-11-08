package ru.kode.pathfinder.android.ui.compose.screen.urllist

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import ru.kode.pathfinder.android.ui.compose.FakeStore
import ru.kode.pathfinder.android.ui.compose.configurationId
import ru.kode.pathfinder.android.ui.compose.dev
import ru.kode.pathfinder.android.ui.compose.login
import ru.kode.pathfinder.android.ui.compose.prod
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps
import ru.kode.pathfinder.android.ui.compose.testConfiguration
import ru.kode.pathfinder.android.ui.compose.users
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlListViewModelTest {
  private val store = FakeStore().also { runBlocking { it.saveConfiguration(testConfiguration) } }
  private val intents = Intents()
  private val viewModel = UrlListViewModel(store).also { it.attach(intents) }

  @After
  fun tearDown() {
    viewModel.destroy()
  }

  @Test
  fun `initial content shows the active environment and its urls`() {
    val content = awaitContent { it.urls.isNotEmpty() }

    assertEquals(dev, content.activeEnvironment)
    assertEquals(listOf(configurationId(dev, users), configurationId(dev, login)), content.urls.map { it.id })
    assertNull(content.showActiveEnvSelector)
  }

  @Test
  fun `environment selector is shown and dismissed`() {
    awaitContent { it.urls.isNotEmpty() }

    intents.changeEnvironment()
    assertEquals(listOf(dev, prod), awaitContent { it.showActiveEnvSelector != null }.showActiveEnvSelector)

    intents.dismissActiveEnvSelector()
    awaitContent { it.showActiveEnvSelector == null }
  }

  @Test
  fun `selecting an environment hides the selector and switches the store`() {
    awaitContent { it.urls.isNotEmpty() }
    intents.changeEnvironment()
    awaitContent { it.showActiveEnvSelector != null }

    intents.setActiveEnvironment(prod.id)

    val content =
      awaitContent { it.activeEnvironment == prod && it.urls.all { url -> url.id.value.startsWith("prod_") } }
    assertNull(content.showActiveEnvSelector)
    assertEquals(prod.id, runBlocking { store.readActiveEnvironmentId() })
  }

  @Test
  fun `path variables are edited, saved closing the editor, and dismissed`() {
    awaitContent { it.urls.isNotEmpty() }
    val id = configurationId(dev, users)

    intents.editPathVariables(id)
    val editing = awaitContent { it.showPathVariableEditor != null }
    assertEquals(id, editing.editedId)
    assertEquals(listOf(PathVariableEditorProps.Variable("id", "")), editing.showPathVariableEditor?.variables)

    intents.saveEditedPathVariables(listOf(PathVariableEditorProps.Variable("id", "42")))
    val saved = awaitContent { content -> content.urls.first { it.id == id }.pathVariableValues == mapOf("id" to "42") }
    assertNull(saved.showPathVariableEditor)
    assertNull(saved.editedId)

    intents.editPathVariables(id)
    awaitContent { it.showPathVariableEditor != null }
    intents.dismissEditPathVariablesDialog()
    val dismissed = awaitContent { it.showPathVariableEditor == null }
    assertNull(dismissed.editedId)
  }

  @Test
  fun `query parameters are edited, saved closing the editor, and dismissed`() {
    awaitContent { it.urls.isNotEmpty() }
    val id = configurationId(dev, users)

    intents.editQueryParameters(id)
    val editing = awaitContent { it.showQueryParameterEditor != null }
    assertEquals(id, editing.editedId)
    assertEquals(listOf(QueryParameterEditorProps.Parameter("lang", "")), editing.showQueryParameterEditor?.parameters)

    intents.saveEditedQueryParameters(listOf(QueryParameterEditorProps.Parameter("lang", "en")))
    val saved = awaitContent { content ->
      content.urls.first { it.id == id }.queryParameterValues ==
        mapOf("lang" to "en")
    }
    assertNull(saved.showQueryParameterEditor)
    assertNull(saved.editedId)

    intents.editQueryParameters(id)
    awaitContent { it.showQueryParameterEditor != null }
    intents.dismissEditQueryParametersDialog()
    val dismissed = awaitContent { it.showQueryParameterEditor == null }
    assertNull(dismissed.editedId)
  }

  @Test
  fun `active environment changed outside the panel is shown`() {
    awaitContent { it.urls.isNotEmpty() }

    runBlocking { store.changeActiveEnvironment(prod.id) }

    awaitContent { it.activeEnvironment == prod && it.urls.all { url -> url.id.value.startsWith("prod_") } }
  }

  // the view model runs on Dispatchers.Default, so wait in real time
  private fun awaitContent(predicate: (ViewState.Content) -> Boolean): ViewState.Content = runBlocking {
    withTimeout(30_000) { viewModel.viewStateFlow.mapNotNull { it.content }.first(predicate) }
  }
}
