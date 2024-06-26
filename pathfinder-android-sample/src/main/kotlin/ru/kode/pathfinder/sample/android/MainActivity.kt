package ru.kode.pathfinder.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.PathFinder
import ru.kode.pathfinder.UrlSpec
import ru.kode.pathfinder.UrlSpecId
import ru.kode.pathfinder.android.store.SqlDelightStore
import ru.kode.pathfinder.android.ui.compose.createConfigurationPanelController

class MainActivity : ComponentActivity() {
  private var showConfigurator by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val configuration = createConfiguration()
    setContent {
      val pathFinder by produceState(initialValue = null as PathFinder?) {
        val store = SqlDelightStore(this@MainActivity)
        value = PathFinder
          .create(store, configuration)
      }
      val controller = remember(pathFinder) {
        pathFinder?.createConfigurationPanelController()
      }
      if (showConfigurator) {
        controller?.Content()
      } else {
        if (pathFinder != null) {
          MainScreen(
            pathFinder = pathFinder!!,
            urlSpecId = configuration.urlSpecs[1].id,
            onConfigure = { showConfigurator = !showConfigurator },
            onChangeBaseUrl = { changeBaseUrl(pathFinder!!, it) }
          )
        }
      }
    }
  }

  private fun changeBaseUrl(pathFinder: PathFinder, baseUrl: String) {
    pathFinder.updateEachEnvironment { env ->
      env.copy(baseUrl = baseUrl)
    }
  }

  override fun onBackPressed() {
    if (showConfigurator) {
      showConfigurator = false
    } else {
      super.onBackPressed()
    }
  }

  @Composable
  private fun MainScreen(
    pathFinder: PathFinder,
    urlSpecId: UrlSpecId,
    onConfigure: () -> Unit,
    onChangeBaseUrl: (String) -> Unit,
  ) {
    Column(
      modifier = Modifier
        .background(color = Color(0x55ACC7E7))
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = "Current Environment", style = MaterialTheme.typography.h6)
      val currentEnvironment by pathFinder.currentEnvironment().collectAsState(initial = pathFinder.currentEnvironment)
      Text(text = currentEnvironment.name, style = MaterialTheme.typography.h3)
      Spacer(modifier = Modifier.height(32.dp))
      val pathVariables = mapOf("categoryId" to "HR", "userId" to "42")
      Text(text = "I send path-parameters", style = MaterialTheme.typography.h6)
      Text(
        text = pathVariables.entries.joinToString("\n") { it.key + " → " + it.value },
        textAlign = TextAlign.Center,
        fontSize = 20.sp,
      )
      Spacer(modifier = Modifier.height(32.dp))
      Text(text = "URL", style = MaterialTheme.typography.h6)
      val url by produceState(initialValue = "", currentEnvironment, showConfigurator) {
        value = pathFinder.buildUrl(urlSpecId, pathVariables = pathVariables)
      }
      Text(
        text = url,
        fontSize = 20.sp,
      )

      Spacer(modifier = Modifier.height(64.dp))

      var baseUrl by remember(currentEnvironment.baseUrl) {
        mutableStateOf(TextFieldValue(currentEnvironment.baseUrl))
      }
      TextField(value = baseUrl, onValueChange = { baseUrl = it })

      Button(onClick = { onChangeBaseUrl(baseUrl.text) }) {
        Text("Change baseUrl")
      }

      Spacer(modifier = Modifier.height(32.dp))

      Button(onClick = onConfigure) {
        Text("Configure")
      }
    }
  }

  private fun createConfiguration(): Configuration {
    return Configuration(
      environments = listOf(
        Environment(
          id = EnvironmentId("mock"),
          name = "Mock",
          baseUrl = "https://mock.project.ru/v1",
          queryParameters = setOf("__example", "__code"),
        ),
        Environment(
          id = EnvironmentId("prod"),
          name = "Production",
          baseUrl = "https://api.project.ru/v1",
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

private fun PathFinder.currentEnvironment() = callbackFlow {
  val listener = object : PathFinder.Listener {
    override fun onEnvironmentSwitch(environmentId: EnvironmentId) {
      trySend(this@currentEnvironment.currentEnvironment)
    }
  }
  addListener(listener)
  awaitClose {
    removeListener(listener)
  }
}
