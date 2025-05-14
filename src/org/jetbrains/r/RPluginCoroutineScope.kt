package org.jetbrains.r

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope


internal object RPluginCoroutineScope {
  fun getScope(project: Project): CoroutineScope = RPluginProjectCoroutineScope.getInstance(project).coroutineScope

  fun getApplicationScope(): CoroutineScope = RPluginAppCoroutineScope.getInstance().coroutineScope
}

@Service(Service.Level.PROJECT)
private class RPluginProjectCoroutineScope(val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): RPluginProjectCoroutineScope = project.service()
  }
}

@Service(Service.Level.APP)
private class RPluginAppCoroutineScope(val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(): RPluginAppCoroutineScope = service()
  }
}