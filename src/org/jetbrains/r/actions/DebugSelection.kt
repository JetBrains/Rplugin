/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory


class DebugSelection : REditorActionBase(
  RBundle.message("debug.selection.action.text"),
  RBundle.message("debug.selection.action.description"),
  AllIcons.Actions.Execute) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selection = REditorActionUtil.getSelectedCode(e) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess { it.debugger.executeDebugSource(selection.file, selection.range) }
      .onError { ex -> RNotificationUtil.notifyConsoleError(project, ex.message) }
    RConsoleToolWindowFactory.show(project)
  }
}
