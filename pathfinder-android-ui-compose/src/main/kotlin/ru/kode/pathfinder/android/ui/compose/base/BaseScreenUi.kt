package ru.kode.pathfinder.android.ui.compose.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel

@Composable
fun <VS : Any, VI : ViewIntents> BaseScreenUi(
  viewModel: ViewModel<VS, VI>,
  screenIntents: VI,
  content: @Composable (state: VS, intents: VI) -> Unit,
) {
  MviComponent(viewModel = viewModel, intents = screenIntents, content = content)
}
