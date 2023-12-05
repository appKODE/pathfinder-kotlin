import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlinAndroid)
  id("com.android.library")
  alias(libs.plugins.sqlDelight)
  `maven-publish`
}

android {
  compileSdk = 34

  namespace = "ru.kode.pathfinder.android.store"

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
  databases {
    create("PathFinderDatabase") {
      packageName.set("ru.kode.pathfinder.android.store")
    }
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
