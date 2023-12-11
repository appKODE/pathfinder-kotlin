package ru.kode.pathfinder.android.ui.compose.screen.urllist

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

class UrlListViewModel(
  private val store: Store
) : ViewModel<ViewState, Intents>() {

  override fun buildMachine() = machine<ViewState> {
    initial = ViewState() to null

    onEach(store.environments()) {
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
      intent(Intents::changeEnvironment).flatMapLatest { store.environments() }
    ) {
      transitionTo { state, environments ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = environments)
        }
      }
    }

    onEach(intent(Intents::dismissActiveEnvSelector)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = null)
        }
      }
    }

    onEach(intent(Intents::setActiveEnvironment)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showActiveEnvSelector = null)
        }
      }
      action { _, _, environmentId ->
        store.changeActiveEnvironment(environmentId)
      }
    }

    onEach(
      store.activeEnvironmentId().filterNotNull()
        .map { id -> store.readEnvironmentById(id) }
        .filterNotNull()
    ) {
      transitionTo { state, environment ->
        state.updateContentSafe { content ->
          content?.copy(activeEnvironment = environment)
        }
      }
    }

    onEach(
      store.activeEnvironmentId().filterNotNull()
        .flatMapLatest { id -> store.urlConfigurations(id) }
    ) {
      transitionTo { state, configs ->
        state.updateContentSafe { content ->
          content?.copy(urls = configs)
        }
      }
    }

    onEach(intent(Intents::editPathVariables)) {
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

    onEach(intent(Intents::dismissEditPathVariablesDialog)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showPathVariableEditor = null, editedId = null)
        }
      }
    }

    onEach(intent(Intents::saveEditedPathVariables)) {
      action { state, _, variables ->
        store.updatePathVariables(
          state.content?.editedId ?: error("no editedId in state"),
          variables.associate { it.name to it.value }
        )
      }
    }

    onEach(intent(Intents::editQueryParameters)) {
      transitionTo { state, id ->
        state.updateContent { content ->
          val parameters = content.urls
            .first { it.id == id }
            .queryParameterValues
            .map { (name, value) ->
              QueryParameterEditorProps.Parameter(
                name = name,
                value = value
              )
            }
          content.copy(
            editedId = id,
            showQueryParameterEditor = QueryParameterEditorProps(parameters = parameters)
          )
        }
      }
    }

    onEach(intent(Intents::dismissEditQueryParametersDialog)) {
      transitionTo { state, _ ->
        state.updateContent { content ->
          content.copy(showQueryParameterEditor = null, editedId = null)
        }
      }
    }

    onEach(intent(Intents::saveEditedQueryParameters)) {
      action { state, _, parameters ->
        store
          .updateQueryParameters(
            state.content?.editedId ?: error("no editedId in state"),
            parameters.associate { it.name to it.value }
          )
      }
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
