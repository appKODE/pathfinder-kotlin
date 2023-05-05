package ru.kode.pathfinder.android.ui.compose

import androidx.compose.runtime.Composable
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.screen.urllist.UrlListScreen

class ConfigurationPanelController(
  store: Store,
) {
  private val screen = UrlListScreen(store)

  @Composable
  fun Content() {
    screen.Content()
  }
}
