package com.intellij.r.psi.rinterop

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise

@Service(Service.Level.PROJECT)
class RInteropCoroutineScope(private val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): RInteropCoroutineScope =
      project.service()

    fun getCoroutineScope(project: Project): CoroutineScope =
      getInstance(project).coroutineScope

    /**
     * bridge for transition between coroutines and old code which expects promise
     */
    fun <T> wrapIntoPromise(project: Project, body: suspend () -> T): Promise<T> {
      return getCoroutineScope(project).async<T> {
        body()
      }.asCompletableFuture().asPromise()
    }
  }
}