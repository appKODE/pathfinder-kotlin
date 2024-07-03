import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
  }
}

plugins {
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  id("com.android.library") version "8.1.0" apply false

  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.spotless)

  `maven-publish`
  signing
}

allprojects {
  repositories {
    mavenCentral()
    mavenLocal()
    google()
  }
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("!**/build/**/*.*")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "120",
          "ktlint_standard_trailing-comma-on-call-site" to "disabled",
          "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        ),
      )
    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "120",
          "ij_kotlin_allow_trailing_comma_on_declaration_site" to "true",
        ),
      )
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }

  plugins.withType<MavenPublishPlugin> {
    logger.warn("maven for ${project.name}")
    apply(plugin = "org.gradle.signing")

    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper> {
      apply(plugin = libs.plugins.dokka.get().pluginId)
      val dokkaHtml by tasks.existing(DokkaTask::class)

      val javadocJar by tasks.registering(Jar::class) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Assembles a jar archive containing the Javadoc API documentation."
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
      }

      configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
        // explicitApi()

        jvm {
          mavenPublication {
            artifact(javadocJar.get())
          }
        }
      }
    }

    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
      apply(plugin = libs.plugins.dokka.get().pluginId)

      val dokkaHtml by tasks.existing(DokkaTask::class)

      val dokkaJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
      }

      val sourcesJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
        archiveClassifier.set("sources")
        from(project.the<SourceSetContainer>()["main"].allSource)
      }

      configure<PublishingExtension> {
        publications.withType<MavenPublication> {
          artifact(dokkaJar)
          artifact(sourcesJar)
        }
      }
    }

    configure<PublishingExtension> {
      repositories {
        maven {
          name = "Central"
          val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
          val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
          val versionName: String by project
          url = if (versionName.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
          credentials {
            username = project.findProperty("NEXUS_USERTOKEN_NAME")?.toString()
            password = project.findProperty("NEXUS_USERTOKEN_PASSWORD")?.toString()
          }
        }
      }

      publications.withType<MavenPublication> {
        val versionName: String by project
        val pomGroupId: String by project
        groupId = pomGroupId
        version = versionName
        pom {
          val pomDescription: String by project
          val pomUrl: String by project
          val pomName: String by project
          description.set(pomDescription)
          url.set(pomUrl)
          name.set(pomName)
          scm {
            val pomScmUrl: String by project
            val pomScmConnection: String by project
            val pomScmDevConnection: String by project
            url.set(pomScmUrl)
            connection.set(pomScmConnection)
            developerConnection.set(pomScmDevConnection)
          }
          licenses {
            license {
              val pomLicenseName: String by project
              val pomLicenseUrl: String by project
              val pomLicenseDist: String by project
              name.set(pomLicenseName)
              url.set(pomLicenseUrl)
              distribution.set(pomLicenseDist)
            }
          }
          developers {
            developer {
              val pomDeveloperId: String by project
              val pomDeveloperName: String by project
              id.set(pomDeveloperId)
              name.set(pomDeveloperName)
            }
          }
        }
      }

      configure<SigningExtension> {
        sign(publications)
      }
    }
  }
}
