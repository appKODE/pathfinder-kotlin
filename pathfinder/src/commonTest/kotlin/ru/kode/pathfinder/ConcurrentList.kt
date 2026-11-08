package ru.kode.pathfinder

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

// common replacement of CopyOnWriteArrayList: listeners are called on a background thread
@OptIn(ExperimentalAtomicApi::class)
class ConcurrentList<T> : AbstractList<T>() {
  private val items = AtomicReference(emptyList<T>())

  override val size: Int get() = items.load().size
  override fun get(index: Int): T = items.load()[index]

  fun add(item: T) = items.update { it + item }
}
