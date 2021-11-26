package ru.kode.pathfinder.android.ui.compose.screen.urllist

import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.base.BaseScreenUi
import ru.kode.pathfinder.android.ui.compose.base.Screen
import ru.kode.pathfinder.android.ui.mvi.BasePresenter
import ru.kode.pathfinder.android.ui.screen.urllist.UrlListPresenter
import ru.kode.pathfinder.android.ui.screen.urllist.ViewIntents
import ru.kode.pathfinder.android.ui.screen.urllist.ViewState

internal class UrlListScreen :
  Screen<ViewState, ViewIntents> {

  override fun createPresenter(store: Store): BasePresenter<ViewState, ViewIntents, *> {
    return UrlListPresenter(store)
  }

  override fun createUi(): BaseScreenUi<ViewState, ViewIntents> {
    return UrlListUi()
  }
}
