package ru.kode.pathfinder.android.ui.mvi

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import ru.dimsuz.unicorn.reactivex.Machine
import ru.dimsuz.unicorn.reactivex.MachineDsl
import ru.dimsuz.unicorn.reactivex.machine
import timber.log.Timber

abstract class BasePresenter<VS : Any, VI : BaseViewIntents, A : Any> {

  private val viewStateRelay = BehaviorRelay.create<VS>()
  private val postViewAttachEventRelay = PublishRelay.create<Boolean>()
  private val intentBinders = arrayListOf<IntentBinder<VI>>()
  private var isFirstViewAttach = true
  private var viewStateSubscription: Disposable? = null
  private var attachedViewIntentsSubscription: CompositeDisposable? = null
  protected var machine: Machine<VS, A>? = null
    private set

  protected abstract fun MachineDsl<VS, A>.buildMachine()

  private fun bindIntents() {
    machine = machine(actionsScheduler = AndroidSchedulers.mainThread()) {
      buildMachine()
    }
    viewStateSubscription = machine!!.states
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          viewStateRelay.accept(it)
        },
        {
          throw IllegalStateException("exception while reducing view state", it)
        }
      )
  }

  protected fun postViewAttachEvents(skipFirstAttach: Boolean): Observable<Unit> {
    return postViewAttachEventRelay
      .filter { isFirstViewAttach -> !(isFirstViewAttach && skipFirstAttach) }
      .map { }
  }

  fun detachView() {
    attachedViewIntentsSubscription?.dispose()
    attachedViewIntentsSubscription = null
  }

  fun destroy() {
    detachView()
    viewStateSubscription?.dispose()
    viewStateSubscription = null
    machine = null
    postDestroy()
  }

  protected open fun postDestroy() = Unit

  @Suppress("UNCHECKED_CAST") // internally type of payload is irrelevant
  private fun <I : Any> intentInternal(bindOp: (VI) -> UiIntentFactory): Observable<I> {
    val binder = IntentBinder(
      bindOp,
      PublishRelay.create()
    )
    intentBinders.add(binder)
    return binder.relay as Observable<I>
  }

  @Suppress("UNCHECKED_CAST") // we actually know the type of payload
  @JvmName("intent1")
  fun <I : Any> intent(bindOp: (VI) -> UiIntentFactory1<I>): Observable<I> {
    return intentInternal(bindOp)
  }

  @Suppress("UNCHECKED_CAST") // we actually know the type of payload
  @JvmName("intent0")
  fun intent(bindOp: (VI) -> UiIntentFactory0): Observable<Unit> {
    return intentInternal(bindOp)
  }

  fun attachView(view: MviView<VS, VI>) {
    check(view.intents === view.intents) {
      "Expected View.intents to always return the same instance, internal error"
    }

    if (isFirstViewAttach) {
      bindIntents()
    }

    val intentSubscriptions = CompositeDisposable()
    intentBinders.forEach { binder ->
      val intent = binder.intent(view.intents)
      intentSubscriptions.add(
        view.intents.stream
          .filter { intent.isOwnerOf(it) }
          .subscribe(
            {
              binder.relay.accept(it.payload)
            },
            {
              throw IllegalStateException("intent \"${intent.name}\" has thrown an exception", it)
            }
          )
      )
    }

    intentSubscriptions.add(
      viewStateRelay.subscribe {
        view.render(it)
      }
    )

    intentSubscriptions.add(
      view.intents.stream
        .subscribe(
          { intent ->
            val screenName = this.javaClass.name.takeLastWhile { it != '.' }.takeWhile { it != '$' }
            Timber.d("[$screenName, intent.name=${intent.name}]")
          },
          {}
        )
    )

    attachedViewIntentsSubscription = intentSubscriptions

    postViewAttachEventRelay.accept(isFirstViewAttach)

    if (isFirstViewAttach) {
      isFirstViewAttach = false
    }
  }

  private data class IntentBinder<VI : Any>(
    val intent: (VI) -> UiIntentFactory,
    val relay: PublishRelay<Any>,
  )
}
