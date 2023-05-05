package ru.kode.pathfinder.android.ui.compose.screen.urllist

import androidx.compose.runtime.Composable
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.base.Screen

internal class UrlListScreen(store: Store) : Screen {
  private val viewModel = UrlListViewModel(store)

  @Composable
  override fun Content() {
    UrlListUi(viewModel)
  }

  override fun destroy() {
    viewModel.destroy()
  }
}
