/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory


class DebugSelection : REditorRunActionBase() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.editor ?: return
    val selection = REditorActionUtil.getSelectedCode(editor) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess { console ->
        console.executeActionHandler.fireBeforeExecution()
        runInEdt {
          console.appendCommandText(selection.code.trim { it <= ' ' })
          console.rInterop.replSourceFile(selection.file, debug = true, textRange = selection.range)
        }
      }
      .onError { ex -> RNotificationUtil.notifyConsoleError(project, ex.message) }
    RConsoleToolWindowFactory.show(project)
  }
}
