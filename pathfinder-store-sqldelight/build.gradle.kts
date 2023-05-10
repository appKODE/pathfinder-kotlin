import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
  id("com.squareup.sqldelight")
  `maven-publish`
}

android {
  compileSdk = 33

  defaultConfig {
    minSdk = 26
    aarMetadata {
      minCompileSdk = 26
    }
  }

  publishing {
    singleVariant("release") {
      withSourcesJar()
      withJavadocJar()
    }
  }
}

publishing {
  publications {
    register<MavenPublication>("release") {
      afterEvaluate {
        from(components["release"])
      }
    }
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
  implementation(libs.sqlDelightCoroutines)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-store-sqldelight"
}
