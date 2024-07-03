# Changelog

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


