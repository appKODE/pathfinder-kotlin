package ru.kode.pathfinder.android.ui.compose.base

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.kode.amvi.viewmodel.ViewIntents

internal abstract class BaseScreenUi<VS : Any, VI : ViewIntents>(screenIntents: VI) {
  private var viewState by mutableStateOf<VS?>(value = null)

  fun render(viewState: VS) {
    this.viewState = viewState
  }

  val intents = screenIntents

  @Composable
  fun Content() {
    MaterialTheme {
      viewState?.also { Content(it) }
    }
  }

  @Composable
  protected abstract fun Content(viewState: VS)
}
