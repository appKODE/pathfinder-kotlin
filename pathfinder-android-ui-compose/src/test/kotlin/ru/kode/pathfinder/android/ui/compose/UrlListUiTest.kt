package ru.kode.pathfinder.android.ui.compose

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.kode.pathfinder.EnvironmentFallbackConfiguration
import ru.kode.pathfinder.PathFinder
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UrlListUiTest {
  @get:Rule
  val composeRule = createComposeRule()

  private val store = FakeStore()

  @Before
  fun setUp() {
    val controller = runBlocking { PathFinder.create(store, testConfiguration) }.createConfigurationPanelController()
    composeRule.setContent { controller.Content() }
    waitForText("users/{id}")
  }

  @Test
  fun `renders the active environment and urls with the host of an additional base url`() {
    composeRule.onNodeWithText("Active Environment").assertExists()
    composeRule.onNodeWithText("Dev").assertExists()
    composeRule.onNodeWithText("User").assertExists()
    composeRule.onNodeWithText("login").assertExists()
    composeRule.onNodeWithText("Login").assertExists()
    // only the url with a baseUrlKey shows a host
    composeRule.onNodeWithText("auth.dev.example.com").assertExists()
    composeRule.onNodeWithText("dev.example.com").assertDoesNotExist()
  }

  @Test
  fun `tapping a url expands and collapses its details`() {
    composeRule.onNodeWithText("Path variables:").assertDoesNotExist()

    composeRule.onNodeWithText("users/{id}").performClick()
    composeRule.onNodeWithText("Path variables:").assertExists()
    composeRule.onNodeWithText("Query parameters:").assertExists()
    assertEquals(2, composeRule.onAllNodesWithText("Not defined").fetchSemanticsNodes().size)

    composeRule.onNodeWithText("users/{id}").performClick()
    composeRule.onNodeWithText("Path variables:").assertDoesNotExist()
  }

  @Test
  fun `path variables are edited in a dialog and saved`() {
    composeRule.onNodeWithText("users/{id}").performClick()
    composeRule.onAllNodesWithText("Edit")[0].performClick()
    waitForText("Edit variables")

    composeRule.onNode(hasSetTextAction()).performTextReplacement("42")
    composeRule.onNodeWithText("Save").performClick()

    waitForText("id=42")
    composeRule.onNodeWithText("Edit variables").assertDoesNotExist()
    val stored = runBlocking { store.readUrlConfiguration(users.id, dev.id) }
    assertEquals(mapOf("id" to "42"), stored?.pathVariableValues)
  }

  @Test
  fun `query parameters are edited in a dialog and saved`() {
    composeRule.onNodeWithText("users/{id}").performClick()
    composeRule.onAllNodesWithText("Edit")[1].performClick()
    waitForText("Edit parameters")

    composeRule.onNode(hasSetTextAction() and hasText("lang")).performTextReplacement("en")
    composeRule.onNodeWithText("Save").performClick()

    waitForText("lang=en")
    composeRule.onNodeWithText("Edit parameters").assertDoesNotExist()
  }

  @Test
  fun `cancel closes the editors without saving`() {
    composeRule.onNodeWithText("users/{id}").performClick()

    composeRule.onAllNodesWithText("Edit")[0].performClick()
    waitForText("Edit variables")
    composeRule.onNode(hasSetTextAction()).performTextReplacement("42")
    composeRule.onNodeWithText("Cancel").performClick()
    waitForNoText("Edit variables")

    composeRule.onAllNodesWithText("Edit")[1].performClick()
    waitForText("Edit parameters")
    composeRule.onNodeWithText("Cancel").performClick()
    waitForNoText("Edit parameters")

    assertEquals(2, composeRule.onAllNodesWithText("Not defined").fetchSemanticsNodes().size)
  }

  @Test
  fun `environment is changed in the selector`() {
    composeRule.onNodeWithText("Change").performClick()
    waitForText("Change environment")

    composeRule.onNodeWithText("Prod").performClick()

    waitForText("Prod")
    waitForNoText("Change environment")
    composeRule.onNodeWithText("auth.dev.example.com").assertDoesNotExist()
    assertEquals(prod.id, runBlocking { store.readActiveEnvironmentId() })
  }

  @Test
  fun `shows fallback base urls`() {
    composeRule.onNodeWithText("Fallback base urls").assertDoesNotExist()

    runBlocking {
      store.saveFallbackConfigurations(
        listOf(EnvironmentFallbackConfiguration("A", listOf("https://a0.test", "https://a1.test"), 1000))
      )
    }

    waitForText("Fallback base urls")
    composeRule.onNodeWithText("A, timeout 1000 ms").assertExists()
    composeRule.onNodeWithText("https://a0.test\nhttps://a1.test").assertExists()
  }

  // the view model runs on Dispatchers.Default which compose test idling does not track;
  // generous timeout for the cold Robolectric start on CI, the wait returns as soon as the condition holds
  private fun waitForText(text: String) = composeRule.waitUntil(30_000) {
    composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
  }

  private fun waitForNoText(text: String) = composeRule.waitUntil(30_000) {
    composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
  }
}
