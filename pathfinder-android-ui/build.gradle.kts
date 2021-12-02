import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
  id("com.android.library")
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

  api(fileTree(File(rootDir, "libs")))

  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.bundles.coroutines)
  implementation(libs.timber)
  implementation(libs.unicornRxJava2)
  implementation(libs.rxRelay)
  implementation(libs.rxJava2)
  implementation(libs.rxAndroid)

  testImplementation(libs.bundles.koTest)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.moduleName = "pathfinder-android"
}
