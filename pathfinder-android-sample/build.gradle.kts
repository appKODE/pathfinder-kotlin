plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinCompose)
}

android {
  compileSdk = libs.versions.compileSdk.get().toInt()

  namespace = "ru.kode.pathfinder.sample.android"

  defaultConfig {
    minSdk = 26
    targetSdk = libs.versions.targetSdk.get().toInt()
  }

  buildFeatures {
    compose = true
  }

  buildTypes {
    debug {
      isDebuggable = true
      isMinifyEnabled = false
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":pathfinder-android-ui-compose"))
  implementation(project(":pathfinder-store-sqldelight"))

  implementation(libs.bundles.coroutines)
  implementation(libs.bundles.compose)
  implementation(libs.activityCompose)
  implementation(libs.androidXCoreKtx)
  implementation(libs.appCompat)
}
