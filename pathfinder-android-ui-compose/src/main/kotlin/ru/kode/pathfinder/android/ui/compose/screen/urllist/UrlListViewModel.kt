package ru.kode.pathfinder.android.ui.compose.screen.urllist

import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store

class UrlListViewModel(
  private val store: Store
) : ViewModel<ViewState, Intents>() {
  override fun buildMachine() = machine {
    initial = ViewState() to null
  }
}
