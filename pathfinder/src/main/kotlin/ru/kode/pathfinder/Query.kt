package ru.kode.pathfinder

interface Query<out ResultType> {
  fun addListener(listener: Listener)
  fun removeListener(listener: Listener)

  fun execute(): ResultType

  interface Listener {
    fun queryResultsChanged()
  }
}

fun <T1, T2> Query<T1>.map(transform: (T1) -> T2): Query<T2> {
  return object : Query<T2> {
    override fun addListener(listener: Query.Listener) {
      this@map.addListener(listener)
    }

    override fun removeListener(listener: Query.Listener) {
      this@map.removeListener(listener)
    }

    override fun execute(): T2 {
      return transform(this@map.execute())
    }
  }
}
