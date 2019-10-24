// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.vfs.LocalFileSystem
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.run.RRunner
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.run.RCommandLineState
import org.jetbrains.r.run.configuration.RRunConfiguration

class RDebugRunner : RRunner() {
  override fun getRunnerId() = "RDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return super.canRun(executorId, profile) && executorId == DefaultDebugExecutor.EXECUTOR_ID
  }

  override fun doExecute(state: RCommandLineState, environment: ExecutionEnvironment) {
    val console = state.console
    val configuration = environment.runProfile as RRunConfiguration
    val scriptPath = configuration.scriptPath
    try {
      val sourceFile = LocalFileSystem.getInstance().findFileByPath(scriptPath) ?:
                       throw RDebuggerException(RBundle.message("debugger.file.not.found", scriptPath))
      console.debugger.executeDebugSource(sourceFile)
    } catch (e: RDebuggerException) {
      if (e.message != null) console.print(e.message + "\n", ConsoleViewContentType.ERROR_OUTPUT)
    }
  }
}

