package ru.kode.pathfinder.android.ui.store

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Scheduler
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.kode.pathfinder.Query
import java.util.concurrent.atomic.AtomicBoolean

@CheckReturnValue
fun <T : Any> Query<T>.asObservable(scheduler: Scheduler = Schedulers.io()): Observable<Query<T>> {
  return Observable.create(QueryOnSubscribe(this)).observeOn(scheduler)
}

private class QueryOnSubscribe<T : Any>(
  private val query: Query<T>,
) : ObservableOnSubscribe<Query<T>> {
  override fun subscribe(emitter: ObservableEmitter<Query<T>>) {
    val listenerAndDisposable = QueryListenerAndDisposable(emitter, query)
    query.addListener(listenerAndDisposable)
    emitter.setDisposable(listenerAndDisposable)
    emitter.onNext(query)
  }
}

private class QueryListenerAndDisposable<T : Any>(
  private val emitter: ObservableEmitter<Query<T>>,
  private val query: Query<T>,
) : AtomicBoolean(), Query.Listener, Disposable {
  override fun queryResultsChanged() {
    emitter.onNext(query)
  }

  override fun isDisposed() = get()

  override fun dispose() {
    if (compareAndSet(false, true)) {
      query.removeListener(this)
    }
  }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T>>.mapToOptional(): Observable<Optional<T>> {
  return map { it.execute().toOptional() }
}
