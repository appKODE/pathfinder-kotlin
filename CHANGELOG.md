# Changelog

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


