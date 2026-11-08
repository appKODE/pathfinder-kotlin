plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinCompose)
}

android {
  compileSdk = libs.versions.compileSdk.get().toInt()

  namespace = "ru.kode.pathfinder.android.ui.compose"

  defaultConfig {
    minSdk = 26
    aarMetadata {
      minCompileSdk = 26
    }
  }

  buildFeatures {
    compose = true
  }
}

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder-android-compose")
    optIn.addAll(
      "androidx.compose.material.ExperimentalMaterialApi",
      "kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(libs.bundles.compose)
  implementation(libs.amviViewModel)
  implementation(libs.amviCompose)
}
