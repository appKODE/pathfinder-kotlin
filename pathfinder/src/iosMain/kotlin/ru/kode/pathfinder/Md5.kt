package ru.kode.pathfinder

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH

// CC_MD5 is deprecated by Apple for cryptography; here it is only a configuration checksum
@OptIn(ExperimentalForeignApi::class, ExperimentalUnsignedTypes::class)
@Suppress("DEPRECATION")
internal actual fun md5(input: ByteArray): ByteArray {
  val digest = UByteArray(CC_MD5_DIGEST_LENGTH)
  input.usePinned { src ->
    digest.usePinned { dst ->
      CC_MD5(if (input.isEmpty()) null else src.addressOf(0), input.size.convert(), dst.addressOf(0))
    }
  }
  return digest.asByteArray()
}
