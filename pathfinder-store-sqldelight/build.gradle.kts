import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
  id("com.squareup.sqldelight")
}

android {
  compileSdk = 31

  defaultConfig {
    minSdk = 26
    targetSdk = 31
  }
}


sqldelight {
  database("PathFinderDatabase") {
    packageName = "ru.kode.pathfinder.android.store"
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.sqlDelightAndroidDriver)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-store-sqldelight"
}
