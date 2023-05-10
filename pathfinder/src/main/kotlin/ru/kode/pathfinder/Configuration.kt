package ru.kode.pathfinder

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

data class Configuration(
  val environments: List<Environment>,
  val urlSpecs: List<UrlSpec>,
  val defaultEnvironmentId: EnvironmentId,
) {
  fun computeChecksum(): String {
    val envKey = buildString {
      append(environments.size)
      environments.forEach { e ->
        append(e.id)
        append(e.name)
        append(e.queryParameters?.joinToString().orEmpty())
        append(e.baseUrl)
      }
    }
    val urlsKey = buildString {
      append(urlSpecs.size)
      urlSpecs.forEach { spec ->
        append(spec.id)
        append(spec.httpMethod.name)
        append(spec.pathTemplate)
        append(spec.name)
      }
    }
    return try {
      val digest = MessageDigest.getInstance("MD5")
      digest.update(envKey.toByteArray())
      digest.update(urlsKey.toByteArray())
      digest.update(defaultEnvironmentId.value.toByteArray())
      digest.digest().toHexString()
    } catch (e: NoSuchAlgorithmException) {
      println("md5 algorithm not found, falling back to plain strings")
      envKey + urlsKey
    }
  }
}

private fun ByteArray.toHexString(): String {
  return joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}
