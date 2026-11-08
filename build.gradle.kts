plugins {
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinCompose) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.sqlDelight) apply false
  alias(libs.plugins.vanniktech.maven.publish) apply false
  alias(libs.plugins.spotless)
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**/*.*")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "120",
          "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
          "ktlint_standard_trailing-comma-on-call-site" to "disabled",
          "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        ),
      )
    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "120",
          "ij_kotlin_allow_trailing_comma_on_declaration_site" to "true",
        ),
      )
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  tasks.withType<Test> {
    useJUnitPlatform()
  }
}
