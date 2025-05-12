package org.jetbrains.r

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
internal class RPluginCoroutineScope(val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): RPluginCoroutineScope = project.service()

    fun getScope(project: Project): CoroutineScope = getInstance(project).coroutineScope
  }
}