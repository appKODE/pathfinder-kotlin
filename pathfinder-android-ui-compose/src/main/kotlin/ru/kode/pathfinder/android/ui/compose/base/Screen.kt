package ru.kode.pathfinder.android.ui.compose.base

import androidx.compose.runtime.Composable

internal interface Screen {
  @Composable
  fun Content()
  fun destroy()
}
