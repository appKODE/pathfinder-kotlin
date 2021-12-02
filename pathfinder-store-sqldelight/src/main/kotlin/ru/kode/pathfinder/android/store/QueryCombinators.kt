package ru.kode.pathfinder.android.store

import ru.kode.pathfinder.Query

internal fun <T1, T2, T3, R> combineLatest(
  query1: Query<T1>,
  query2: Query<T2>,
  query3: Query<T3>,
  combiner: (T1, T2, T3) -> R,
): Query<R> {
  return object : Query<R>, Query.Listener {
    private val listeners = mutableListOf<Query.Listener>()

    init {
      query1.addListener(this)
      query2.addListener(this)
      query3.addListener(this)
    }

    override fun addListener(listener: Query.Listener) {
      listeners.add(listener)
    }

    override fun removeListener(listener: Query.Listener) {
      listeners.remove(listener)
    }

    override fun execute(): R {
      return combiner(query1.execute(), query2.execute(), query3.execute())
    }

    override fun queryResultsChanged() {
      listeners.forEach { it.queryResultsChanged() }
    }
  }
}

internal fun <T1, T2> Query<T1>.flatMap(transform: (T1) -> Query<T2>): Query<T2> {
  return object : Query<T2> {
    override fun addListener(listener: Query.Listener) {
      this@flatMap.addListener(listener)
    }

    override fun removeListener(listener: Query.Listener) {
      this@flatMap.removeListener(listener)
    }

    override fun execute(): T2 {
      val t1 = this@flatMap.execute()
      return transform(t1).execute()
    }
  }
}
