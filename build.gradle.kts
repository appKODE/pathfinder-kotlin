import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
  }
}

plugins {
  kotlin("jvm") version libs.versions.kotlin apply false
  kotlin("android") version libs.versions.kotlin apply false
  id("com.android.library") version "7.4.2" apply false

  alias(libs.plugins.sqlDelight) apply false
  alias(libs.plugins.spotless)

  `maven-publish`
  signing
  id("org.jetbrains.dokka") version "1.4.32"
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("!**/build/**/*.*")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "120",
          "ij_kotlin_allow_trailing_comma_on_call_site" to "true",

        )
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
          "ij_kotlin_allow_trailing_comma_on_call_site" to "true",

          )
      )
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "signing")

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}
