package ru.kode.pathfinder

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
        // appended only when set: keeps checksums (and stored overrides) of existing configurations
        if (e.additionalBaseUrls.isNotEmpty()) {
          // same format as java.util.TreeMap.toString() used before KMP, keeps checksums stable
          e.additionalBaseUrls.entries.sortedBy { it.key }.joinTo(this, ", ", "{", "}") { "${it.key}=${it.value}" }
        }
      }
    }
    val urlsKey = buildString {
      append(urlSpecs.size)
      urlSpecs.forEach { spec ->
        append(spec.id)
        append(spec.httpMethod.name)
        append(spec.pathTemplate)
        append(spec.name)
        spec.baseUrlKey?.let(::append)
      }
    }
    return md5((envKey + urlsKey + defaultEnvironmentId.value).encodeToByteArray()).toHexString()
  }
}

private fun ByteArray.toHexString(): String = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

internal expect fun md5(input: ByteArray): ByteArray
