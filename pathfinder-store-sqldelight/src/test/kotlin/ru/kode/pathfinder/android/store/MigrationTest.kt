package ru.kode.pathfinder.android.store

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.UrlSpec
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Upgrade from 0.11.x: its schema is version 2.
 */
class MigrationTest {
  private val dev = Environment(
    id = EnvironmentId("dev"),
    name = "Dev",
    baseUrl = "https://dev.example.com",
    queryParameters = setOf("token", "lang"),
  )
  private val prod = Environment(id = EnvironmentId("prod"), name = "Prod", baseUrl = "https://example.com")
  private val users = UrlSpec(pathTemplate = "users/{id}", httpMethod = UrlSpec.HttpMethod.GET, name = "User")
  private val login = UrlSpec(pathTemplate = "login", httpMethod = UrlSpec.HttpMethod.POST, name = "Login")

  // the same configuration as in ConfigurationTest, its checksum is the one 0.11.1 has stored
  private val configuration = Configuration(
    environments = listOf(dev, prod),
    urlSpecs = listOf(users, login),
    defaultEnvironmentId = dev.id,
  )
  private val checksum011 = "47760b4410d464cd01e3022c72608ea7"

  private val file = createTempFile(suffix = ".db").toFile()
    .also { File("src/main/sqldelight/databases/2.db").copyTo(it, overwrite = true) }
  private val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")

  @AfterTest
  fun tearDown() {
    driver.close()
    file.delete()
  }

  private fun insert011Rows() {
    listOf(
      "INSERT INTO Configuration VALUES (1, 'prod', '$checksum011')",
      "INSERT INTO Environment VALUES ('dev', 'Dev', 'https://dev.example.com', 'token:::lang', 1, 1)",
      "INSERT INTO Environment VALUES ('prod', 'Prod', 'https://example.com', NULL, 2, 2)",
    ).forEach { driver.execute(null, it, 0) }
    listOf("dev", "prod").forEachIndexed { index, env ->
      listOf(users, login).forEach { spec ->
        val id = "${spec.id.value}_$env"
        driver.execute(
          null,
          "INSERT INTO UrlConfiguration VALUES ('$id', '${spec.id.value}', '$env', '${spec.pathTemplate}', " +
            "'${spec.name}', '${spec.httpMethod}', $index, $index)",
          0,
        )
        spec.pathVariables.forEach { name ->
          val value = if (env == "prod") "42" else ""
          driver.execute(null, "INSERT INTO PathVariable VALUES ('$id', '$name', '$value', 0, 0)", 0)
        }
        if (env == "dev") {
          listOf("token", "lang").forEach { name ->
            driver.execute(null, "INSERT INTO QueryParameter VALUES ('$id', '$name', '', 0, 0)", 0)
          }
        }
      }
    }
  }

  @Test
  fun `0_11_x data is readable after the migration`() = runTest {
    insert011Rows()

    PathFinderDatabase.Schema.migrate(driver, 2, 3)
    val store = SqlDelightStore(driver)

    assertEquals(EnvironmentId("prod"), store.readActiveEnvironmentId())
    assertEquals(listOf(dev, prod), store.readEnvironments())
    val prodUsers = store.readUrlConfiguration(users.id, prod.id)!!
    assertEquals(mapOf("id" to "42"), prodUsers.pathVariableValues)
    assertNull(prodUsers.baseUrlKey)
    assertEquals(
      mapOf("token" to "", "lang" to ""),
      store.readUrlConfiguration(login.id, dev.id)!!.queryParameterValues
    )
  }

  @Test
  fun `saving the same configuration after the migration keeps 0_11_x overrides`() = runTest {
    insert011Rows()
    PathFinderDatabase.Schema.migrate(driver, 2, 3)
    val store = SqlDelightStore(driver)

    store.saveConfiguration(configuration)

    assertEquals(EnvironmentId("prod"), store.readActiveEnvironmentId())
    assertEquals(mapOf("id" to "42"), store.readUrlConfiguration(users.id, prod.id)!!.pathVariableValues)
  }
}
