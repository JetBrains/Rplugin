/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import org.jetbrains.r.console.RConsoleManagerImpl
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

class DumpRInteropMessages : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val logPath = PathManager.getLogPath()
    val project = e.project ?: return
    val json = RConsoleManagerImpl.getInstance(project).currentConsoleOrNull?.rInterop?.rInteropGrpcLogger?.toJson() ?: return
    Paths.get(logPath, dateFormat.format(Date()) + ".json").toFile().writeText(json)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = ApplicationManager.getApplication().isInternal
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}