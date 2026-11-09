package ru.kode.pathfinder.android.ui.compose.screen.urllist

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.PathVariableEditorProps
import ru.kode.pathfinder.android.ui.compose.screen.urllist.entity.QueryParameterEditorProps

class UrlListViewModel(private val store: Store) : ViewModel<ViewState, Intents>() {

  override fun buildMachine() = machine<ViewState> {
    initial = ViewState() to null

    // one flow, so the active environment and its urls always arrive together: separate flows on
    // Dispatchers.Default raced, and urls emitted before the content existed were lost
    onEach(
      store.activeEnvironmentId().filterNotNull().flatMapLatest { id ->
        combine(
          store.environments(),
          store.urlConfigurations(id),
          store.fallbackConfigurations(),
        ) { environments, urls, fallbacks ->
          environments.find { it.id == id }?.let { Triple(it, urls, fallbacks) }
        }
      }.filterNotNull()
    ) {
      transitionTo { state, (environment, urls, fallbacks) ->
        state.copy(
          content = state.content?.copy(activeEnvironment = environment, urls = urls, fallbacks = fallbacks)
            ?: ViewState.Content(activeEnvironment = environment, urls = urls, fallbacks = fallbacks)
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

    // save closes the editor itself: a separate dismiss intent may be reduced before this one and clear editedId
    onEach(intent(Intents::saveEditedPathVariables)) {
      transitionTo { state, _ ->
        state.updateContent { content -> content.copy(showPathVariableEditor = null, editedId = null) }
      }
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
      transitionTo { state, _ ->
        state.updateContent { content -> content.copy(showQueryParameterEditor = null, editedId = null) }
      }
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

private inline fun ViewState.updateContent(map: (content: ViewState.Content) -> ViewState.Content): ViewState =
  this.copy(
    content = this.content?.let { map(it) } ?: error("unexpected: content is null")
  )
