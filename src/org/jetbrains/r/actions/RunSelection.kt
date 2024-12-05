/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rmarkdown.RMarkdownFileType


/**
 * Event handler for the "Run Selection" action within an Arc code editor - runs the currently selected text within the
 * current REPL.
 */
abstract class RunSelectionBase : REditorActionBase() {
  abstract val isDebug: Boolean

  override fun update(e: AnActionEvent) {
    super.update(e)
    val project = e.project ?: return
    e.presentation.isEnabled = e.presentation.isEnabled &&
                               RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.isAlive != false
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.editor ?: return
    val selection = REditorActionUtil.getSelectedCode(editor) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync.onSuccess { console ->
      ConsoleHistoryController.addToHistory(console, selection.code)
      when (selection.file.fileType) {
        RFileType -> executeForRFile(console, selection)
        RMarkdownFileType -> executeForRMarkdownFile(project, selection.file, editor, selection.range)
      }
    }
    .onError { ex -> RNotificationUtil.notifyConsoleError(project, ex.message) }
    RConsoleToolWindowFactory.focusOnCurrentConsole(project)
  }

  private fun executeForRFile(console: RConsoleView, selection: REditorActionUtil.SelectedCode) {
    val debugCommand = RDebuggerUtil.getFirstDebugCommand(console.project, selection.file, selection.range)
    console.executeActionHandler.splitAndExecute(selection.code, isDebug = isDebug, sourceFile = selection.file,
                                                 sourceStartOffset = selection.range.startOffset,
                                                 firstDebugCommand = debugCommand)
  }

  private fun executeForRMarkdownFile(project: Project, virtualFile: VirtualFile, editor: Editor, range: TextRange) {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
    RunChunkHandler.getInstance(project).runAllChunks(psiFile, editor, range.startOffset, range.endOffset, runSelectedCode = true, isDebug = isDebug)
  }
}

class RunSelection : RunSelectionBase() {
  override val isDebug = false
}

class DebugSelection : RunSelectionBase() {
  override val isDebug = true
}
