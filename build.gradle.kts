import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
  }
}

plugins {
  kotlin("jvm") version libs.versions.kotlin apply false
  kotlin("android") version libs.versions.kotlin apply false
  id("com.android.library") version "7.0.3" apply false
  alias(libs.plugins.sqlDelight) apply false

  `maven-publish`
  signing
  id("org.jetbrains.dokka") version "1.4.32"
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }
}

val ktlint: Configuration by configurations.creating

dependencies {
  ktlint(libs.ktlint) {
    // this is required due to https://github.com/pinterest/ktlint/issues/1114
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling::class, Bundling.EXTERNAL))
    }
  }
}

val ktlintCheck by tasks.creating(JavaExec::class) {
  description = "Check Kotlin code style."
  classpath = ktlint
  group = "verification"
  main = "com.pinterest.ktlint.Main"
  args = listOf("**/src/**/*.kt", "!**/build/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
  description = "Fix Kotlin code style deviations."
  classpath = ktlint
  group = "verification"
  main = "com.pinterest.ktlint.Main"
  args = listOf("-F", "**/src/**/*.kt", "!**/build/**/*.kt")
}

subprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "signing")

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }

}
