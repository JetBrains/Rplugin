/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.notifications.RNotificationUtil
import com.intellij.ui.AppUIUtil
import org.jetbrains.r.console.RConsoleManagerImpl
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleViewImpl

abstract class RRunActionBase : REditorActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = e.virtualFile ?: return
    AppUIUtil.invokeOnEdt {
      RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {}
    }
    RConsoleManagerImpl.getInstance(project).currentConsoleAsync.onSuccess { console ->
      if (REditorActionUtil.isRunningCommand(console)) {
        RNotificationUtil.notifyConsoleError(project, RBundle.message("notification.console.busy"))
      }
      console.executeActionHandler.fireBeforeExecution()
      console.executeActionHandler.fireBusy()
      console.interpreter.prepareForExecutionAsync().onProcessed {
        doExecute(console, file)
      }
    }.onError {
      RNotificationUtil.notifyConsoleError(project, it.message)
    }
    RConsoleToolWindowFactory.focusOnCurrentConsole(project)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = e.presentation.isEnabled && !REditorActionUtil.isRunningCommand(e.project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  abstract fun doExecute(console: RConsoleViewImpl, file: VirtualFile)
}

class RRunAction : RRunActionBase() {
  override fun doExecute(console: RConsoleViewImpl, file: VirtualFile) {
    console.rInterop.replSourceFile(file)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.virtualFile?.let { e.presentation.text = RBundle.message("run.file.action.text", it.name) }
  }
}

class RDebugAction : RRunActionBase() {
  override fun doExecute(console: RConsoleViewImpl, file: VirtualFile) {
    console.rInterop.replSourceFile(file, debug = true)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.virtualFile?.let { e.presentation.text = RBundle.message("debug.file.action.text", it.name) }
  }
}
