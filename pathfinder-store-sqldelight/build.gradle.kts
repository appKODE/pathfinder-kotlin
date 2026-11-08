plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.sqlDelight)
}

android {
  compileSdk = libs.versions.compileSdk.get().toInt()

  namespace = "ru.kode.pathfinder.android.store"

  defaultConfig {
    minSdk = 26
    aarMetadata {
      minCompileSdk = 26
    }
  }
}

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder-store-sqldelight")
  }
}

sqldelight {
  databases {
    create("PathFinderDatabase") {
      packageName.set("ru.kode.pathfinder.android.store")
      schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
    }
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(libs.sqlDelightAndroidDriver)
  implementation(libs.sqlDelightCoroutines)
}
