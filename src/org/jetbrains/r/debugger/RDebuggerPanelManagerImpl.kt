package org.jetbrains.r.debugger

import com.intellij.openapi.project.Project
import com.intellij.r.psi.debugger.RDebuggerPanelManager
import com.intellij.r.psi.rinterop.RVar
import org.jetbrains.r.console.RConsoleManager

class RDebuggerPanelManagerImpl(private val project: Project) : RDebuggerPanelManager {
  override fun navigate(rVar: RVar) {
    RConsoleManager.getInstance(project).currentConsoleOrNull?.debuggerPanel?.navigate(rVar)
  }
}