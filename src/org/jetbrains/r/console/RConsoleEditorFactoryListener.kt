/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.r.psi.RFileType
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile

class RConsoleEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val project = event.editor.project ?: return
    val file = FileDocumentManager.getInstance().getFile(event.editor.document) ?: return
    val fileTypeRegistry = FileTypeRegistry.getInstance()

    if (fileTypeRegistry.isFileOfType(file, RFileType) || RMarkdownVirtualFile.isRMarkdownOrQuarto(fileTypeRegistry, file)) {
      val toolWindowManager = ToolWindowManager.getInstance(project)

      val outerModalityState = ModalityState.defaultModalityState()

      toolWindowManager.invokeLater {
        val toolWindow = toolWindowManager.getToolWindow(RConsoleToolWindowFactory.ID)
        if (toolWindow != null && toolWindow.contentManager.contentCount == 0) {
          /**
           * see R-1549
           * currentConsoleAsync triggers file saving in [com.intellij.r.psi.interpreter.RInterpreter.prepareForExecutionAsync]
           * but RConsoleManager expects that currentConsoleAsync will be called after creation of RConsoleToolWindowFactory
           * see [org.jetbrains.r.console.RConsoleManager.runSingleConsole]
           * Ideally RConsole and toolwindow should not be bound in a such way
           */
          invokeLater(outerModalityState) {
            RConsoleManager.getInstance(project).currentConsoleAsync.onSuccess {
              toolWindowManager.invokeLater {
                toolWindow.show { }
              }
            }
          }
        }
      }
    }
  }
}