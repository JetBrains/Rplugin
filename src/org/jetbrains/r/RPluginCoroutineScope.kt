package org.jetbrains.r

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
internal class RPluginCoroutineScope(val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: com.intellij.openapi.project.Project): RPluginCoroutineScope = project.service()
  }
}