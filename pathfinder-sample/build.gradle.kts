plugins {
  kotlin("jvm")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.bundles.coroutines)

  testImplementation(libs.bundles.koTest)
}

tasks {
  compileKotlin {
    kotlinOptions.moduleName = "pathfinder-sample"
  }
}
