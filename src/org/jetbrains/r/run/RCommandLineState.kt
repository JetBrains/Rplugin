// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction

class RCommandLineState(environment: ExecutionEnvironment) : CommandLineState(environment) {
  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    return object : ExecutionResult {
      override fun getExecutionConsole() = null
      override fun getProcessHandler() = null
      override fun getActions() = emptyArray<AnAction>()
    }
  }

  override fun startProcess(): ProcessHandler {
    throw NotImplementedError()
  }
}
