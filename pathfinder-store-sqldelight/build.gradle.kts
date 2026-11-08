plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.sqlDelight)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.kover)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

kotlin {
  jvmToolchain(17)

  jvm {
    compilerOptions {
      moduleName.set("pathfinder-store-sqldelight")
    }
  }

  androidLibrary {
    namespace = "ru.kode.pathfinder.android.store"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 26
  }

  iosArm64()
  iosSimulatorArm64()
  iosX64()

  sourceSets {
    commonMain.dependencies {
      api(project(":pathfinder"))
      // was exposed by the SQLDelight 2.0 plugin in 0.11.x, consumers rely on it
      api(libs.sqlDelightRuntime)
      implementation(libs.sqlDelightCoroutines)
    }
    androidMain.dependencies {
      implementation(libs.sqlDelightAndroidDriver)
    }
    iosMain.dependencies {
      implementation(libs.sqlDelightNativeDriver)
    }
    jvmTest.dependencies {
      implementation(kotlin("test-junit5"))
      implementation(libs.coroutines.test)
      implementation(libs.sqlDelightJvmDriver)
    }
  }
}

sqldelight {
  databases {
    create("PathFinderDatabase") {
      packageName.set("ru.kode.pathfinder.android.store")
      schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
    }
  }
}

mavenPublishing {
  coordinates(artifactId = "pathfinder-store-sqldelight")

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
