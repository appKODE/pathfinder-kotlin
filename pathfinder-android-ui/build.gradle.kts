import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library") version "7.0.3"
}

android {
  compileSdk = 31

  defaultConfig {
    minSdk = 26
    targetSdk = 31
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.bundles.coroutines)
  implementation(libs.timber)
  implementation(libs.unicornRxJava2)
  implementation(libs.rxRelay)

  testImplementation(libs.bundles.koTest)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-android"
}
