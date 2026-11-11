# Changelog

## 0.14.1 - 2026-11-11
* `pathfinder-store-sqldelight` and `pathfinder-android-ui-compose` lower Android `minSdk` from 26 to 23 (the AndroidX
  minimum): they use no API above 23, apps with `minSdk` 23-25 failed the manifest merge

## 0.14.0 - 2026-11-09
* Fallback base urls: `PathFinder.updateFallbackEnvironments(configurations)` stores ordered fallback base
  urls and a per-attempt timeout (`EnvironmentFallbackConfiguration`) under an app-chosen key (e.g. a country code),
  `DEFAULT_ENVIRONMENT_PARAMETERS_KEY` is used for a key without its own configuration.
  `updateFallbackKey(key)` selects the configuration, `readFallbackPlan(primaryBaseUrl, allowFallbacks)` returns
  the base urls to try for one request (`FallbackRequestPlan`), `markFallbackSuccess(key, baseUrl)` makes the
  base url which served a request go first in the next plans
* `SqlDelightStore` persists fallback configurations and last successful base urls. Store migration 3 adds the
  `FallbackConfiguration` and `LastSuccessfulFallback` tables, stored environments and overrides are kept
* `Store` has fallback methods with default implementations which do not persist: custom stores compile unchanged
* Add `PathFinder.currentEnvironmentFlow: StateFlow<Environment>`: the active environment and its changes
* Debug panel shows the fallback base urls (read-only)
* Debug panel: the url list could open empty or show a stale environment, because the environment and
  its urls were collected by separate flows racing each other

## 0.13.0 - 2026-11-08
* Kotlin Multiplatform: `pathfinder` and `pathfinder-store-sqldelight` target Android, JVM, iosArm64,
  iosSimulatorArm64, iosX64. Coordinates are unchanged, Gradle resolves the platform artifact.
  `pathfinder-android-ui-compose` stays Android-only
* `SqlDelightStore(driver: SqlDriver)` is public: pass any SQLDelight driver created with `PathFinderDatabase.Schema`
* iOS: `SqlDelightStore()` creates a store backed by `NativeSqliteDriver` (`pathfinder.db`)
* Configuration checksums are unchanged, stored environments and overrides survive the upgrade

### Breaking changes
* Android: `SqlDelightStore(context)` is a top-level function instead of a constructor.
  Kotlin sources compile unchanged, Java callers use `SqlDelightStoreFactoryKt.SqlDelightStore(context)`
* `pathfinder` is published as an Android AAR (`pathfinder-android`) / JVM jar (`pathfinder-jvm`) instead of one jar

## 0.12.0 - 2026-11-08
* `PathFinder.create` returns a ready instance: `buildUrl` works right after it returns
* `currentEnvironment` and listeners follow changes of the active environment (e.g. its `baseUrl`), not only switches
* Add `updateEnvironment(id, updater)` and `saveConfiguration(configuration)` to `PathFinder`
* Add `buildUrl(environmentId, id, …)` to build a url for an environment other than the active one
* Add named base urls: `Environment.additionalBaseUrls` and `UrlSpec.baseUrlKey`.
  The debug panel shows the host of a url which uses a named base url
* Fix: `SqlDelightStore.replaceEnvironment` changed the stored configuration version, so the next launch wiped all
  stored environments and overrides
* Fix: replacing with a new environment id created no urls for it, so `buildUrl` failed for that environment
* Fix: saving path variables or query parameters in the debug panel could fail with `no editedId in state`
* Publish to Maven Central via Central Portal; update dependencies (Kotlin 2.3.20, AGP 9.0.1, Compose 1.10.6,
  SQLDelight 2.4.1). Store migration 2 adds the new columns

### Breaking changes
* `switchEnvironment`, `replaceEnvironment`, `updateEachEnvironment` are `suspend` and finish the change before
  returning (they were fire-and-forget)
* `Store.changeActiveEnvironment` is `suspend`
* Listeners are no longer called once after `create`: there is nothing to wait for anymore.
  They are called on a switch or a change of the active environment
* `saveConfiguration` with a configuration which differs from the stored one still replaces all stored environments
  and overrides
* Installs where an earlier `replaceEnvironment` already changed the stored version get one reset of stored
  environments and overrides on the first launch with 0.12.0

### Migration
Dynamic base url (instead of calling `Store.replaceEnvironment` + `Store.changeActiveEnvironment`):

```kotlin
pathFinder.updateEnvironment(DYNAMIC_ENV_ID) { it.copy(baseUrl = "https://$host") }
pathFinder.switchEnvironment(DYNAMIC_ENV_ID)
```

Urls with another base url, either per call:

```kotlin
pathFinder.buildUrl(API_ENV_ID, urlSpecId)
```

or declared once:

```kotlin
Environment(
  id = EnvironmentId("prod"),
  name = "Production",
  baseUrl = "https://api.example.com/v2",
  additionalBaseUrls = mapOf("v1" to "https://api.example.com/v1"),
)
UrlSpec(pathTemplate = "config", httpMethod = UrlSpec.HttpMethod.GET, name = "Config", baseUrlKey = "v1")
```

## 0.11.1 - 2024-07-03
* Add "pathfinder_" prefix to all string resources. This should fix conflicts with common names like "action_cancel" which client application could also have.

## 0.11.0 - 2024-06-05
* Update dependencies

## 0.10.0 - 2023-12-11
* Add the ability to replace/update environment properties dynamically

```kotlin
private fun changeBaseUrl(pathFinder: PathFinder, baseUrl: String) {
  pathFinder.updateEachEnvironment { env ->
    env.copy(baseUrl = baseUrl)
  }
}
```

```kotlin
private fun replace(pathFinder: PathFinder) {
  // replace single environment properties with a new set
  pathFinder.replaceEnvironment(myNewEnvironment)
}
```


