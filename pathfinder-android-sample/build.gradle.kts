import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.application")
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
  implementation(project(":pathfinder-android-ui-compose"))

  implementation(libs.bundles.coroutines)
  implementation(libs.bundles.compose)
  implementation(libs.activityCompose)
  implementation(libs.androidXCoreKtx)
  implementation(libs.appCompat)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-android-sample"
}
