package ru.kode.pathfinder

data class Configuration(
  val version: Int,
  val environments: List<Environment>,
  val urlSpecs: List<UrlSpec>,
  val defaultEnvironmentId: EnvironmentId,
)
