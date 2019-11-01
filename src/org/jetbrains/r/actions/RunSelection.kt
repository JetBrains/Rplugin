/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory


/**
 * Event handler for the "Run Selection" action within an Arc code editor - runs the currently selected text within the
 * current REPL.
 */
class RunSelection : REditorActionBase(
  RBundle.message("run.selection.action.text"),
  RBundle.message("run.selection.action.description"),
  AllIcons.Actions.Execute) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selection = REditorActionUtil.getSelectedCode(e) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess { runInEdt { runWriteAction { it.executeText(selection.code.trim { it <= ' ' }) } } }
      .onError { ex -> RNotificationUtil.notifyConsoleError(project, ex.message) }
    RConsoleToolWindowFactory.show(project)
  }
}
