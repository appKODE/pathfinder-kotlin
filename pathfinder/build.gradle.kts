import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.kover)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

kotlin {
  jvmToolchain(17)

  compilerOptions {
    moduleName.set("pathfinder")
  }
}

mavenPublishing {
  coordinates(artifactId = "pathfinder")
  configure(KotlinJvm(javadocJar = JavadocJar.Empty()))

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
  api(libs.bundles.coroutines)

  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.coroutines.test)
}
