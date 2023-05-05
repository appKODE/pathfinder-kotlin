rootProject.name = "pathfinder-kotlin"

include("pathfinder")
include("pathfinder-android-ui-compose")
include("pathfinder-android-sample")
include("pathfinder-store-sqldelight")

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
