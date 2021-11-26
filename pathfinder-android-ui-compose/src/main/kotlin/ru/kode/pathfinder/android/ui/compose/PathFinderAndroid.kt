package ru.kode.pathfinder.android.ui.compose

import ru.kode.pathfinder.PathFinder

fun PathFinder.createConfigurationPanelController(): ConfigurationPanelController {
  return ConfigurationPanelController(this.store)
}
