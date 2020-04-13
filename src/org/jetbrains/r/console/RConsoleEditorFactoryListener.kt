/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.r.RFileType
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class RConsoleEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val project = event.editor.project ?: return
    val file = FileDocumentManager.getInstance().getFile(event.editor.document) ?: return
    if (file.fileType == RFileType || file.fileType == RMarkdownFileType) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    toolWindowManager.invokeLater(Runnable {
      val toolWindow = toolWindowManager.getToolWindow(RConsoleToolWindowFactory.ID)
      if (toolWindow != null && toolWindow.contentManager.contentCount == 0) {
        RConsoleManager.getInstance(project).currentConsoleAsync.onSuccess {
          invokeLater {
            toolWindow.show { }
          }
        }
      }
    })
    }
  }
}