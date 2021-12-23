import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
}

android {
  compileSdk = 31

  defaultConfig {
    minSdk = 26
    targetSdk = 31
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
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
  kotlinOptions.freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
}
