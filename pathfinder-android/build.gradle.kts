tasks.withType<Test> {
  useJUnitPlatform()
}

dependencies {
  implementation(kotlin("stdlib"))

  testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
  testImplementation("io.kotest:kotest-assertions-core:4.6.0")
  testImplementation("io.kotest:kotest-property:4.6.0")
}
