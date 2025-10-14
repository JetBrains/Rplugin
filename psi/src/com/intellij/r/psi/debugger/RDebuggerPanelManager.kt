package com.intellij.r.psi.debugger

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.r.psi.rinterop.RVar

interface RDebuggerPanelManager {
  fun navigate(rVar: RVar)

  companion object {
    fun getInstance(project: Project): RDebuggerPanelManager = project.service()
  }
}