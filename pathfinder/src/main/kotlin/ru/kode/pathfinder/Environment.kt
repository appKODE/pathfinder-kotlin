package ru.kode.pathfinder

data class Environment(
  val id: EnvironmentId,
  val name: String,
  val baseUrl: String,
  val queryParameters: Set<String>? = null,
)

@JvmInline
value class EnvironmentId(val value: String)
