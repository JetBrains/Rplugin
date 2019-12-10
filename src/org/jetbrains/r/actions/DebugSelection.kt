/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.debugger.exception.RDebuggerException


class DebugSelection : REditorActionBase() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.editor ?: return
    val selection = REditorActionUtil.getSelectedCode(editor) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess {
        it.executeActionHandler.fireBeforeExecution()
        try {
          it.debugger.executeDebugSource(selection.file, selection.range)
        } catch (e: RDebuggerException) {
          RNotificationUtil.notifyConsoleError(project, e.message)
        }
      }
      .onError { ex -> RNotificationUtil.notifyConsoleError(project, ex.message) }
    RConsoleToolWindowFactory.show(project)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val project = e.project ?: return
    val console = RConsoleManager.getInstance(project).currentConsoleOrNull ?: return
    e.presentation.isEnabled = e.presentation.isEnabled && console.isRunningCommand != true && !console.debugger.isEnabled

  }
}
