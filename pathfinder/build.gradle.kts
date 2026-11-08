plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.kover)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

kotlin {
  jvmToolchain(17)

  jvm {
    compilerOptions {
      moduleName.set("pathfinder")
    }
  }

  androidLibrary {
    namespace = "ru.kode.pathfinder"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21
  }

  iosArm64()
  iosSimulatorArm64()
  iosX64()

  sourceSets {
    commonMain.dependencies {
      api(libs.bundles.coroutines)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.coroutines.test)
    }
  }
}

mavenPublishing {
  coordinates(artifactId = "pathfinder")

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
