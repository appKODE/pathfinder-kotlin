# PathFinder

Builds request URLs from declared environments (base URLs) and URL specs, and lets testers switch environments and
override path variables / query parameters at runtime from a debug panel.

| Artifact | Platforms | Purpose |
|---|---|---|
| `ru.kode.pathfinder:pathfinder` | Android, JVM, iOS | Core: `PathFinder`, `Configuration`, `Store` |
| `ru.kode.pathfinder:pathfinder-store-sqldelight` | Android, JVM, iOS | Persistent `Store` on SQLDelight |
| `ru.kode.pathfinder:pathfinder-android-ui-compose` | Android | Compose debug panel |

Latest version: see [CHANGELOG](CHANGELOG.md) or Maven Central.

## Usage

```kotlin
val store = SqlDelightStore(context) // Android; SqlDelightStore() on iOS; SqlDelightStore(driver) anywhere
val pathFinder = PathFinder.create(store, configuration)
val url = pathFinder.buildUrl(UrlSpecId("users"), pathVariables = mapOf("id" to "1"))

// dynamic base url received at runtime
pathFinder.updateEnvironment(prodId) { it.copy(baseUrl = dynamicHost) }
pathFinder.switchEnvironment(prodId)
```

## Release

1. Bump `versionName` in `gradle.properties`, add a `## X.Y.Z - date` section to `CHANGELOG.md`, merge to `master`.
2. Push tag `vX.Y.Z`: `.github/workflows/release.yml` (macOS runner, GitHub environment `release`) runs tests and
   publishes to Maven Central.
