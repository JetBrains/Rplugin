// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.ConfigurationException
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.run.configuration.RRunConfiguration
import org.jetbrains.r.run.configuration.RRunConfigurationUtils

class RCommandLineState(environment: ExecutionEnvironment,
                        private val myRunConfiguration: RRunConfiguration) : CommandLineState(environment) {
  val console: RConsoleView by lazy {
    RConsoleManager.getInstance(myRunConfiguration.project).currentConsoleAsync.blockingGet(10000)!!
  }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    return DefaultExecutionResult(console, startProcess())
  }

  override fun startProcess(): ProcessHandler {
    checkRunConfiguration()
    return console.rInterop.processHandler
  }

  private fun checkRunConfiguration() {
    try {
      RRunConfigurationUtils.checkConfiguration(myRunConfiguration)
    } catch (e: ConfigurationException) {
      throw ExecutionException(e)
    }
  }
}
