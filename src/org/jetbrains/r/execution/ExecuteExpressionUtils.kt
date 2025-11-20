/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.execution

import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.CapturingProcessRunner
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.r.psi.RPluginUtil
import com.intellij.r.psi.interpreter.RInterpreterLocation
import com.intellij.util.ui.EDT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import java.nio.file.Paths

object ExecuteExpressionUtils {
  private val LOGGER = Logger.getInstance(ExecuteExpressionUtils::class.java)

  fun <E> getListBlockingWithIndicator(
    project: Project,
    title: @NlsContexts.ModalProgressTitle String,
    debugName: String,
    timeout: Int = DEFAULT_TIMEOUT,
    task: suspend () -> List<E>,
  ): List<E> {
    return try {
      runWithModalProgressBlocking(project, title) {
        withTimeout(timeout.toLong()) {
          task()
        }
      }
    }
    catch (e: CancellationException) {
      throw e
    }
    catch (e: Throwable) {
      LOGGER.error("Failed to get list blocking: $debugName", e)
      mutableListOf()
    }
  }

  // taken from: com.jetbrains.python.util.runWithModalBlockingOrInBackground
  fun <T> runWithModalBlockingOrInBackground(project: Project, @NlsSafe msg: String, action: suspend CoroutineScope.() -> T): T {
    if (EDT.isCurrentThreadEdt()) {
      return runWithModalProgressBlocking(project, msg, action)
    }

    return runBlockingMaybeCancellable(action)
  }

  fun <E> getListBlocking(project: Project, debugName: String, timeout: Int = DEFAULT_TIMEOUT, task: suspend () -> List<E>): List<E> {
    return try {
      runWithModalBlockingOrInBackground(project, debugName) {
        withTimeout(timeout.toLong()) {
          task()
        }
      }
    }
    catch (e: CancellationException) {
      throw e
    }
    catch (e: Throwable) {
      LOGGER.error("Failed to get list blocking: $debugName", e)
      mutableListOf()
    }
  }

  fun <R> getSynchronously(title: String, task: () -> R): R {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable {
      task()
    }, title, false, null)
  }

  fun executeScriptInBackground(
    interpreterLocation: RInterpreterLocation,
    relativeScriptPath: String,
    args: List<String>,
    title: String,
    timeout: Int = DEFAULT_TIMEOUT,
    project: Project? = null,
  ): ProcessOutput {
    return getSynchronously(title) {
      executeScript(interpreterLocation, relativeScriptPath, args, timeout, project)
    }
  }

  fun launchScript(
    interpreterLocation: RInterpreterLocation,
    relativeScriptPath: String,
    args: List<String>,
    workingDirectory: String? = null,
    project: Project? = null,
  ): BaseProcessHandler<*> {
    val helper = RPluginUtil.findFileInRHelpers(Paths.get("R", relativeScriptPath).toString())
    val helperOnHost = interpreterLocation.uploadFileToHost(helper)
    val interpreterArgs = RInterpreterUtil.getRunHelperArgs(helperOnHost, args, project)
    return interpreterLocation.runInterpreterOnHost(interpreterArgs, workingDirectory)
  }

  fun executeScript(
    interpreterLocation: RInterpreterLocation,
    relativeScriptPath: String,
    args: List<String>,
    timeout: Int = DEFAULT_TIMEOUT,
    project: Project? = null,
  ): ProcessOutput {
    val process = launchScript(interpreterLocation, relativeScriptPath, args, project = project)
    return CapturingProcessRunner(process).runProcess(timeout)
  }
}