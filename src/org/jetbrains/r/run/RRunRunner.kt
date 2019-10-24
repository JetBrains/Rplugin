// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.text.StringUtil
import icons.org.jetbrains.r.run.RRunner
import org.jetbrains.r.run.configuration.RRunConfiguration

class RRunRunner : RRunner() {
  override fun getRunnerId() = "RRunRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return super.canRun(executorId, profile) && executorId == DefaultRunExecutor.EXECUTOR_ID
  }

  override fun doExecute(state: RCommandLineState, environment: ExecutionEnvironment) {
    val console = state.console
    val configuration = environment.runProfile as RRunConfiguration
    val scriptPath = configuration.scriptPath
    console.rInterop.sourceFile(scriptPath)
  }
}
