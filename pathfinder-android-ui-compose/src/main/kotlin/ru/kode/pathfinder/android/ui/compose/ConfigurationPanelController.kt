package ru.kode.pathfinder.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.base.BaseScreenUi
import ru.kode.pathfinder.android.ui.compose.base.Screen
import ru.kode.pathfinder.android.ui.compose.screen.urllist.UrlListScreen

class ConfigurationPanelController(
  private val store: Store,
) {

  private var currentScreen by mutableStateOf<Screen<*, *>>(UrlListScreen())
  private var currentViewModel by mutableStateOf<ViewModel<*, *>?>(value = null)
  private var currentUi by mutableStateOf<BaseScreenUi<*, *>?>(value = null)

  @Composable
  fun Content() {
    SwitchScreenEffect(screen = currentScreen)
    currentUi?.Content()
  }

  @Composable
  private fun SwitchScreenEffect(screen: Screen<*, *>) {
    LaunchedEffect(screen) {
      currentViewModel?.detach()
      currentViewModel?.destroy()

      val viewModel = screen.createViewModel(store) as ViewModel<*, ViewIntents>
      val ui = screen.createUi()
      viewModel.attach(ui.intents)
      currentScreen = screen
      currentViewModel = viewModel
      currentUi = ui
    }
    DisposableEffect(screen) {
      onDispose {
        currentViewModel?.detach()
        currentViewModel?.destroy()
        currentViewModel = null
        currentUi = null
      }
    }
  }
}
