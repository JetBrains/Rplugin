/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.ui.AppUIUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.run.RCommandLineState
import org.jetbrains.r.run.configuration.RRunConfiguration

abstract class RRunner : GenericProgramRunner<RunnerSettings>() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is RRunConfiguration &&
           !isRunningCommand(RConsoleManager.getInstance(profile.project).currentConsoleOrNull)
  }

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    if (state !is RCommandLineState) return null
    val project = environment.project
    AppUIUtil.invokeOnEdt {
      FileDocumentManager.getInstance().saveAllDocuments()
      RConsoleToolWindowFactory.getToolWindow(project)?.show {}
    }
    ApplicationManager.getApplication().executeOnPooledThread {
      val console = RConsoleManager.getInstance(project).currentConsoleAsync.blockingGet(5000) ?: return@executeOnPooledThread
      if (isRunningCommand(console)) return@executeOnPooledThread
      val configuration = environment.runProfile as RRunConfiguration
      val workingDir = configuration.workingDirectoryPath
      console.rInterop.setWorkingDir(workingDir)
      doExecute(console, environment)
    }
    return null
  }

  protected abstract fun doExecute(console: RConsoleView, environment: ExecutionEnvironment)

  private fun isRunningCommand(console: RConsoleView?): Boolean {
    if (console == null) return false
    return console.debugger.isEnabled || console.isRunningCommand
  }
}