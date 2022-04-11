package ru.kode.pathfinder.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.PathFinder
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.android.store.SqlDelightStore
import ru.kode.pathfinder.android.ui.compose.ConfigurationPanelController
import ru.kode.pathfinder.android.ui.compose.createConfigurationPanelController

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val controller by produceState(initialValue = null as ConfigurationPanelController?) {
        val store = SqlDelightStore(this@MainActivity)
        value = PathFinder
          .create(store, createConfiguration())
          .createConfigurationPanelController()
      }
      controller?.Content()
    }
  }

  private fun createConfiguration(): Configuration {
    return Configuration(
      version = 4,
      environments = listOf(
        Environment(
          id = EnvironmentId("mock"),
          name = "Mock",
          baseUrl = "https://mock.cats.ru/v1",
          queryParameters = setOf("__example", "__code"),
        ),
        Environment(
          id = EnvironmentId("prod"),
          name = "Production",
          baseUrl = "https://api.cats.ru/v1",
          queryParameters = null,
        ),
      ),
      urlSpecs = listOf(
        UrlSpec(
          pathTemplate = "auth/login",
          UrlSpec.HttpMethod.POST,
          name = "Perform login",
        ),
        UrlSpec(
          pathTemplate = "{categoryId}/users/{userId}",
          UrlSpec.HttpMethod.POST,
          name = "Get user by category",
        )
      ),
      defaultEnvironmentId = EnvironmentId("mock"),
    )
  }
}
