package ru.kode.pathfinder.android.ui.store

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.Store
import ru.kode.pathfinder.UrlConfiguration
import ru.kode.pathfinder.map

internal class ReactiveStore(private val store: Store) {

  fun changeActiveEnvironment(id: EnvironmentId): Completable {
    return Completable
      .fromAction { store.changeActiveEnvironment(id) }
      .subscribeOn(Schedulers.io())
  }

  fun activeEnvironmentId(): Observable<Optional<EnvironmentId>> {
    return store.activeEnvironmentId().map { it.toOptional() }.asObservable().map { it.execute() }
  }

  fun findEnvironments(): Observable<List<Environment>> {
    return store.findEnvironments().asObservable().map { it.execute() }
  }

  fun findEnvironmentById(id: EnvironmentId): Observable<Optional<Environment>> {
    return store.findEnvironmentById(id).map { it.toOptional() }.asObservable().map { it.execute() }
  }

  fun findUrlConfigurations(environmentId: EnvironmentId): Observable<List<UrlConfiguration>> {
    return store.findUrlConfigurations(environmentId).asObservable().map { it.execute() }
  }

  fun updatePathVariables(id: UrlConfiguration.Id, pathVariableValues: Map<String, String>): Completable {
    return Completable
      .fromAction { store.updatePathVariables(id, pathVariableValues) }
      .subscribeOn(Schedulers.io())
  }

  fun updateQueryParameters(id: UrlConfiguration.Id, queryParameterValues: Map<String, String>): Completable {
    return Completable
      .fromAction { store.updateQueryParameters(id, queryParameterValues) }
      .subscribeOn(Schedulers.io())
  }
}
