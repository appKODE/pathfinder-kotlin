pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "pathfinder-kotlin"

include("pathfinder")
include("pathfinder-android-ui-compose")
include("pathfinder-android-sample")
include("pathfinder-store-sqldelight")
