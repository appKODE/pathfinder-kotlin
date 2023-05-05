import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
}

android {
  compileSdk = 33

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.bundles.compose)
  implementation(libs.amviViewModel)
  implementation(libs.amviCompose)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-android-compose"
  kotlinOptions.freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
  kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
}
