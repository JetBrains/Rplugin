package com.intellij.r.psi.console

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise

interface RConsoleManager {
  val consoles: List<RConsoleView>

  fun runConsole(requestFocus: Boolean = false, workingDir: String? = null): Promise<RConsoleView>

  companion object {
    fun getInstance(project: Project): RConsoleManager = project.service()
  }
}