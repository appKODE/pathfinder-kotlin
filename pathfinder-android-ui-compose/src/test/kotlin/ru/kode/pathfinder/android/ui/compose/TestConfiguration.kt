package ru.kode.pathfinder.android.ui.compose

import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.UrlSpec

internal val dev = Environment(
  id = EnvironmentId("dev"),
  name = "Dev",
  baseUrl = "https://dev.example.com",
  queryParameters = setOf("lang"),
  additionalBaseUrls = mapOf("auth" to "https://auth.dev.example.com/v1"),
)
internal val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://example.com")
internal val users = UrlSpec(pathTemplate = "users/{id}", httpMethod = UrlSpec.HttpMethod.GET, name = "User")
internal val login = UrlSpec(
  pathTemplate = "login",
  httpMethod = UrlSpec.HttpMethod.POST,
  name = "Login",
  baseUrlKey = "auth",
)
internal val testConfiguration = Configuration(
  environments = listOf(dev, prod),
  urlSpecs = listOf(users, login),
  defaultEnvironmentId = dev.id,
)

// FakeStore id format
internal fun configurationId(environment: Environment, spec: UrlSpec) =
  UrlConfiguration.Id("${environment.id.value}_${spec.id.value}")
