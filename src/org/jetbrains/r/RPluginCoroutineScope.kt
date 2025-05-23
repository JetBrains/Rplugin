package org.jetbrains.r

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope


object RPluginCoroutineScope {
  fun getScope(project: Project): CoroutineScope = RPluginCoroutineScopeImpl.getInstance(project).coroutineScope

  fun getApplicationScope(): CoroutineScope = RPluginCoroutineScopeImpl.getInstance().coroutineScope
}

@Service(Service.Level.PROJECT, Service.Level.APP)
private class RPluginCoroutineScopeImpl(val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): RPluginCoroutineScopeImpl = project.service()

    fun getInstance(): RPluginCoroutineScopeImpl = service()
  }
}
