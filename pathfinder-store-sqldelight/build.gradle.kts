import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.sqlDelight)
  alias(libs.plugins.vanniktech.maven.publish)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

android {
  compileSdk = libs.versions.compileSdk.get().toInt()

  namespace = "ru.kode.pathfinder.android.store"

  defaultConfig {
    minSdk = 26
    aarMetadata {
      minCompileSdk = 26
    }
  }
}

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder-store-sqldelight")
  }
}

sqldelight {
  databases {
    create("PathFinderDatabase") {
      packageName.set("ru.kode.pathfinder.android.store")
      schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
    }
  }
}

mavenPublishing {
  coordinates(artifactId = "pathfinder-store-sqldelight")
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
  // was exposed by the SQLDelight 2.0 plugin in 0.11.x, consumers rely on it
  api(libs.sqlDelightRuntime)

  implementation(libs.sqlDelightAndroidDriver)
  implementation(libs.sqlDelightCoroutines)
}
