package org.jetbrains.r.rinterop

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class RInteropCoroutineScope(private val coroutineScope: CoroutineScope) {

  companion object {
    fun getInstance(project: Project): RInteropCoroutineScope =
      project.getService(RInteropCoroutineScope::class.java)

    fun getCoroutineScope(project: Project): CoroutineScope =
      getInstance(project).coroutineScope
  }
}