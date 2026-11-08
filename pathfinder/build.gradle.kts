plugins {
  alias(libs.plugins.kotlinJvm)
}

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder")
  }
}

dependencies {
  api(libs.bundles.coroutines)

  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.coroutines.test)
}
