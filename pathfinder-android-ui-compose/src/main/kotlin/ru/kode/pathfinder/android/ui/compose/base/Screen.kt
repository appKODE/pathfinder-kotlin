package ru.kode.pathfinder.android.ui.compose.base

import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store

internal interface Screen<VS : Any, VI : ViewIntents> {
  fun createViewModel(store: Store): ViewModel<VS, VI>
  fun createUi(): BaseScreenUi<VS, VI>
}
