/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ThrowableComputable
import org.jetbrains.r.packages.RHelpersUtil
import java.nio.file.Paths

private const val TIMEOUT = 60 * 1000 // 1 min

object ExecuteExpressionUtils {
  fun <R> getSynchronously(title: String, task: () -> R): R {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable<R, Exception> {
      task()
    }, title, false, null)
  }

  fun executeScriptInBackground(rScriptPath: String,
                                relativeScriptPath: String,
                                args: List<String>,
                                title: String,
                                timeout: Int = TIMEOUT): ProcessOutput {
    return getSynchronously<ProcessOutput>(title) {
      executeScript(rScriptPath, relativeScriptPath, args, timeout)
    }
  }

  fun executeScript(rScriptPath: String,
                    relativeScriptPath: String,
                    args: List<String>,
                    timeout: Int = TIMEOUT): ProcessOutput {
    val scriptPath = RHelpersUtil.findFileInRHelpers(Paths.get("R", relativeScriptPath).toString()).absolutePath
    val generalCommandLine = GeneralCommandLine(rScriptPath, scriptPath, *args.toTypedArray())
    return CapturingProcessHandler(generalCommandLine).runProcess(timeout)
  }
}