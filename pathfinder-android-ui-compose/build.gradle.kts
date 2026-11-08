import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.kover)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

android {
  compileSdk = libs.versions.compileSdk.get().toInt()

  namespace = "ru.kode.pathfinder.android.ui.compose"

  defaultConfig {
    minSdk = 26
    aarMetadata {
      minCompileSdk = 26
    }
  }

  buildFeatures {
    compose = true
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder-android-compose")
    optIn.addAll(
      "androidx.compose.material.ExperimentalMaterialApi",
      "kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
  }
}

mavenPublishing {
  coordinates(artifactId = "pathfinder-android-ui-compose")
  configure(AndroidSingleVariantLibrary(javadocJar = JavadocJar.Empty()))

  publishToMavenCentral()
  signAllPublications()

  pom {
    val pomName: String by project
    val pomDescription: String by project
    val pomUrl: String by project
    val pomScmUrl: String by project
    val pomScmConnection: String by project
    val pomScmDevConnection: String by project
    val pomLicenseName: String by project
    val pomLicenseUrl: String by project
    val pomLicenseDist: String by project
    val pomDeveloperId: String by project
    val pomDeveloperName: String by project

    name.set(pomName)
    description.set(pomDescription)
    url.set(pomUrl)

    scm {
      url.set(pomScmUrl)
      connection.set(pomScmConnection)
      developerConnection.set(pomScmDevConnection)
    }

    licenses {
      license {
        name.set(pomLicenseName)
        url.set(pomLicenseUrl)
        distribution.set(pomLicenseDist)
      }
    }

    developers {
      developer {
        id.set(pomDeveloperId)
        name.set(pomDeveloperName)
      }
    }
  }
}

dependencies {
  api(project(":pathfinder"))

  implementation(libs.bundles.compose)
  implementation(libs.amviViewModel)
  implementation(libs.amviCompose)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit4)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.composeUiTestJunit4)
  debugImplementation(libs.composeUiTestManifest)
  // Robolectric and Compose tests are JUnit4, the root project runs tests on the JUnit Platform
  testRuntimeOnly(libs.junitVintageEngine)
}
