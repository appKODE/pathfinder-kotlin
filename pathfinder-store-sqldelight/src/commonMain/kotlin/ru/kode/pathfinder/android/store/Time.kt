package ru.kode.pathfinder.android.store

import kotlin.time.Clock

internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
