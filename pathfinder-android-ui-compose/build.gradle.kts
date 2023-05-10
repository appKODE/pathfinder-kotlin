import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
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

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
  }

  buildFeatures {
    compose = true
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
