@file:Suppress("ktlint:standard:filename")

package ru.kode.pathfinder.android.ui.compose

import ru.kode.pathfinder.PathFinder

fun PathFinder.createConfigurationPanelController(): ConfigurationPanelController =
  ConfigurationPanelController(this.store)
