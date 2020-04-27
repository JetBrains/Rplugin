/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ThrowableComputable
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import java.nio.file.Paths

object ExecuteExpressionUtils {
  private val LOGGER = Logger.getInstance(ExecuteExpressionUtils::class.java)

  fun <E> getListBlockingWithIndicator(
    title: String,
    debugName: String,
    timeout: Int = DEFAULT_TIMEOUT,
    task: () -> Promise<List<E>>
  ): List<E> {
    return getSynchronously(title) {
      getListBlocking(debugName, timeout, task)
    }
  }

  fun <E> getListBlocking(debugName: String, timeout: Int = DEFAULT_TIMEOUT, task: () -> Promise<List<E>>): List<E> {
    return task()
      .onError { LOGGER.error("Failed to get list blocking: $debugName", it) }
      .blockingGet(timeout) ?: emptyList()
  }

  fun <R> getSynchronously(title: String, task: () -> R): R {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable<R, Exception> {
      task()
    }, title, false, null)
  }

  fun executeScriptInBackground(rScriptPath: String,
                                relativeScriptPath: String,
                                args: List<String>,
                                title: String,
                                timeout: Int = DEFAULT_TIMEOUT): ProcessOutput {
    return getSynchronously<ProcessOutput>(title) {
      executeScript(rScriptPath, relativeScriptPath, args, timeout)
    }
  }

  fun executeScript(rScriptPath: String,
                    relativeScriptPath: String,
                    args: List<String>,
                    timeout: Int = DEFAULT_TIMEOUT): ProcessOutput {
    val scriptPath = RPluginUtil.findFileInRHelpers(Paths.get("R", relativeScriptPath).toString()).absolutePath
    val generalCommandLine = GeneralCommandLine(rScriptPath, scriptPath, *args.toTypedArray())
    return CapturingProcessHandler(generalCommandLine).runProcess(timeout)
  }
}