package org.jetbrains.r.lsp

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project

interface RLspStatus {
  fun isRunning(project: Project): Boolean

  companion object {
    private val EP_NAME: ExtensionPointName<RLspStatus> = create("com.intellij.rLspStatus")

    fun isLspRunning(project: Project): Boolean = EP_NAME.extensionList.any { it.isRunning(project) }
  }
}