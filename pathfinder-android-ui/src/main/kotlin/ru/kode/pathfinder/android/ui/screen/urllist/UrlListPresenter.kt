package ru.kode.pathfinder.android.ui.screen.urllist

import com.gojuno.koptional.rxjava2.filterSome
import io.reactivex.disposables.CompositeDisposable
import ru.dimsuz.unicorn.reactivex.MachineDsl
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.android.ui.mvi.BasePresenter
import ru.kode.pathfinder.android.ui.mvi.BaseViewIntents
import ru.kode.pathfinder.android.ui.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.screen.urllist.entity.QueryParameterEditorProps
import ru.kode.pathfinder.android.ui.store.ReactiveStore
import timber.log.Timber

class UrlListPresenter(
  store: Store,
) : BasePresenter<ViewState, ViewIntents, Unit>() {

  private val rxStore = ReactiveStore(store)
  private val subscriptions = CompositeDisposable()

  override fun postDestroy() {
    subscriptions.dispose()
  }

  @Suppress("LongMethod") // contains logic of whole screen
  override fun MachineDsl<ViewState, Unit>.buildMachine() {
    initial = ViewState() to null

    onEach(rxStore.findEnvironments()) {
      transitionTo { state, environments ->
        state.copy(
          content = ViewState.Content(
            activeEnvironment = environments.firstOrNull() ?: error("environment list is empty"),
            urls = emptyList()
          )
        )
      }
    }

    onEach(
      intent(ViewIntents::changeEnvironment)
        .flatMapSingle { rxStore.findEnvironments().firstOrError() }
    ) {
      transitionTo { state, environments ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = environments)
        }
      }
    }

    onEach(intent(ViewIntents::dismissActiveEnvSelector)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = null)
        }
      }
    }

    onEach(intent(ViewIntents::setActiveEnvironment)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = null)
        }
      }
      action { _, _, environmentId ->
        subscriptions.add(
          rxStore.changeActiveEnvironment(environmentId)
            .subscribe({}, { Timber.e(it, "change environment failed") })
        )
      }
    }

    onEach(
      rxStore.activeEnvironmentId().filterSome()
        .flatMapSingle { id -> rxStore.findEnvironmentById(id).filterSome().firstOrError() }
    ) {
      transitionTo { state, environment ->
        state.updateContentSafe { content ->
          content?.copy(activeEnvironment = environment)
        }
      }
    }

    onEach(
      rxStore.activeEnvironmentId().filterSome()
        .switchMap { id -> rxStore.findUrlConfigurations(id) }
    ) {
      transitionTo { state, configs ->
        state.updateContentSafe { content ->
          content?.copy(urls = configs)
        }
      }
    }

    onEach(intent(ViewIntents::editPathVariables)) {
      transitionTo { state, id ->
        state.updateContent { content ->
          val variables = content.urls
            .first { it.id == id }
            .pathVariableValues
            .map { (name, value) -> PathVariableEditorProps.Variable(name = name, value = value) }
          content.copy(
            editedId = id,
            showPathVariableEditor = PathVariableEditorProps(variables = variables)
          )
        }
      }
    }

    onEach(intent(ViewIntents::dismissEditPathVariablesDialog)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showPathVariableEditor = null, editedId = null)
        }
      }
    }

    onEach(intent(ViewIntents::saveEditedPathVariables)) {
      action { state, _, variables ->
        subscriptions.add(
          rxStore
            .updatePathVariables(
              state.content?.editedId ?: error("no editedId in state"),
              variables.associate { it.name to it.value }
            )
            .subscribe({}, { Timber.e(it, "change environment failed") })
        )
      }
    }

    onEach(intent(ViewIntents::editQueryParameters)) {
      transitionTo { state, id ->
        state.updateContent { content ->
          val parameters = content.urls
            .first { it.id == id }
            .queryParameterValues
            .map { (name, value) -> QueryParameterEditorProps.Parameter(name = name, value = value) }
          content.copy(
            editedId = id,
            showQueryParameterEditor = QueryParameterEditorProps(parameters = parameters)
          )
        }
      }
    }

    onEach(intent(ViewIntents::dismissEditQueryParametersDialog)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showQueryParameterEditor = null, editedId = null)
        }
      }
    }

    onEach(intent(ViewIntents::saveEditedQueryParameters)) {
      action { state, _, parameters ->
        subscriptions.add(
          rxStore
            .updateQueryParameters(
              state.content?.editedId ?: error("no editedId in state"),
              parameters.associate { it.name to it.value }
            )
            .subscribe({}, { Timber.e(it, "change environment failed") })
        )
      }
    }
  }

  private inline fun ViewState.updateContent(map: (content: ViewState.Content) -> ViewState.Content): ViewState {
    return this.copy(
      content = this.content?.let { map(it) } ?: error("unexpected: content is null")
    )
  }

  private inline fun ViewState.updateContentSafe(map: (content: ViewState.Content?) -> ViewState.Content?): ViewState {
    return this.copy(
      content = this.content?.let { map(it) }
    )
  }
}

class ViewIntents : BaseViewIntents() {
  val changeEnvironment = intent(name = "changeEnvironment")
  val dismissActiveEnvSelector = intent(name = "dismissActiveEnvSelector")
  val setActiveEnvironment = intent<EnvironmentId>(name = "setActiveEnvironment")
  val editPathVariables = intent<UrlConfiguration.Id>(name = "editPathVariables")
  val dismissEditPathVariablesDialog = intent(name = "dismissEditPathVariablesDialog")
  val saveEditedPathVariables = intent<List<PathVariableEditorProps.Variable>>(name = "saveEditedPathVariables")
  val editQueryParameters = intent<UrlConfiguration.Id>(name = "editQueryParameters")
  val dismissEditQueryParametersDialog = intent(name = "dismissEditQueryParametersDialog")
  val saveEditedQueryParameters = intent<List<QueryParameterEditorProps.Parameter>>(name = "saveEditedQueryParameters")
}

data class ViewState(
  val content: Content? = null
) {
  data class Content(
    val activeEnvironment: Environment,
    val urls: List<UrlConfiguration>,
    val editedId: UrlConfiguration.Id? = null,
    val showActiveEnvSelector: List<Environment>? = null,
    val showPathVariableEditor: PathVariableEditorProps? = null,
    val showQueryParameterEditor: QueryParameterEditorProps? = null,
  )
}
