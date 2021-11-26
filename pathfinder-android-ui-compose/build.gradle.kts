import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library") version "7.0.3"
}

android {
  compileSdk = 31

  defaultConfig {
    minSdk = 26
    targetSdk = 31
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  api(project(":pathfinder"))
  implementation(project(":pathfinder-android-ui"))

  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.bundles.compose)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-android-compose"
}
