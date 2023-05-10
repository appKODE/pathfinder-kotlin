plugins {
  kotlin("jvm")
  `maven-publish`
}

dependencies {
  api(kotlin("stdlib-jdk8"))
  api(libs.bundles.coroutines)

  testImplementation(libs.bundles.koTest)
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

publishing {
  publications {
    register<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

tasks {
  compileKotlin {
    kotlinOptions.moduleName = "pathfinder"
    kotlinOptions.jvmTarget = "1.8"
  }
}
