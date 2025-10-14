/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import java.io.File

interface RInterpreterLocation {
  @Nls
  fun additionalShortRepresentationSuffix(): String = ""

  @Nls
  fun getWidgetSwitchInterpreterActionHeader(): String

  // workingDirectory is a separate parameter and not a part of GeneralCommandLine because it does not work well with remote paths
  fun runProcessOnHost(command: GeneralCommandLine, workingDirectory: String? = null, isSilent: Boolean = false): BaseProcessHandler<*>

  fun runInterpreterOnHost(args: List<String>, workingDirectory: String? = null, environment: Map<String, String>? = null): BaseProcessHandler<*>

  fun uploadFileToHost(file: File, preserveName: Boolean = false): String

  fun createInterpreter(project: Project): Result<RInterpreterBase>

  fun lastModified(): Long? = null

  fun canRead(path: String): Boolean

  fun canWrite(path: String): Boolean

  fun canExecute(path: String): Boolean
}
