package ru.kode.pathfinder.android.ui.compose.base

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.insets.ProvideWindowInsets
import ru.kode.pathfinder.android.ui.mvi.BaseViewIntents
import ru.kode.pathfinder.android.ui.mvi.MviView

internal abstract class BaseScreenUi<VS : Any, VI : BaseViewIntents>(screenIntents: VI) : MviView<VS, VI> {
  private var viewState by mutableStateOf<VS?>(value = null)

  override fun render(viewState: VS) {
    this.viewState = viewState
  }

  override val intents = screenIntents

  @Composable
  fun Content() {
    MaterialTheme {
      ProvideWindowInsets(consumeWindowInsets = false) {
        viewState?.also { Content(it) }
      }
    }
  }

  @Composable
  protected abstract fun Content(viewState: VS)
}
