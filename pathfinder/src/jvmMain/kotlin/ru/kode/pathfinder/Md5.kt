package ru.kode.pathfinder

import java.security.MessageDigest

internal actual fun md5(input: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(input)
