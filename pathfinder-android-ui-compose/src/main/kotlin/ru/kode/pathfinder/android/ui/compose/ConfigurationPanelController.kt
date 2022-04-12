package ru.kode.pathfinder.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.base.BaseScreenUi
import ru.kode.pathfinder.android.ui.compose.base.Screen
import ru.kode.pathfinder.android.ui.compose.screen.urllist.UrlListScreen
import ru.kode.pathfinder.android.ui.mvi.BasePresenter
import ru.kode.pathfinder.android.ui.mvi.MviView

class ConfigurationPanelController(
  private val store: Store,
) {

  private var currentScreen by mutableStateOf<Screen<*, *>>(UrlListScreen())
  private var currentPresenter by mutableStateOf<BasePresenter<*, *, *>?>(value = null)
  private var currentUi by mutableStateOf<BaseScreenUi<*, *>?>(value = null)

  @Composable
  fun Content() {
    SwitchScreenEffect(screen = currentScreen)
    currentUi?.Content()
  }

  @Composable
  private fun SwitchScreenEffect(screen: Screen<*, *>) {
    LaunchedEffect(screen) {
      currentPresenter?.detachView()
      currentPresenter?.destroy()

      val presenter = screen.createPresenter(store)
      val ui = screen.createUi()
      presenter.attachView(ui as MviView<Any, Nothing>)
      currentScreen = screen
      currentPresenter = presenter
      currentUi = ui
    }
    DisposableEffect(screen) {
      onDispose {
        currentPresenter?.detachView()
        currentPresenter?.destroy()
        currentPresenter = null
        currentUi = null
      }
    }
  }
}
