package ru.kode.pathfinder.android.ui.compose.base

import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.mvi.BasePresenter
import ru.kode.pathfinder.android.ui.mvi.BaseViewIntents

internal interface Screen<VS : Any, VI : BaseViewIntents> {
  fun createPresenter(store: Store): BasePresenter<VS, VI, *>
  fun createUi(): BaseScreenUi<VS, VI>
}
