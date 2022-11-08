package ru.kode.pathfinder.android.ui.compose.screen.urllist

import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.base.BaseScreenUi
import ru.kode.pathfinder.android.ui.compose.base.Screen

internal class UrlListScreen :
  Screen<ViewState, Intents> {

  override fun createViewModel(store: Store): ViewModel<ViewState, Intents> {
    return UrlListViewModel(store)
  }

  override fun createUi(): BaseScreenUi<ViewState, Intents> {
    return UrlListUi()
  }
}
