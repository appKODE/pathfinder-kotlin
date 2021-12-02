rootProject.name = "pathfinder-kotlin"

include("pathfinder")
include("pathfinder-android-ui")
include("pathfinder-android-ui-compose")
include("pathfinder-store-sqldelight")
include("pathfinder-sample")

enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }

  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.android.library") {
        useModule("com.android.tools.build:gradle:${requested.version}")
      }
    }
  }
}
