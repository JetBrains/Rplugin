/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rinterop.Service.ExecuteCodeRequest.DebugCommand
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.util.PromiseUtil


/**
 * Event handler for the "Run Selection" action within an Arc code editor - runs the currently selected text within the
 * current REPL.
 */
abstract class RunSelectionBase : REditorActionBase() {
  abstract val isDebug: Boolean

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.editor ?: return
    val selection = REditorActionUtil.getSelectedCode(editor) ?: return
    RConsoleManager.getInstance(project).currentConsoleAsync
      .onSuccess { console ->
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
    var debugCommand = RDebuggerUtil.getFirstDebugCommand(console.project, selection.file, selection.range)
    RConsoleExecuteActionHandler.splitCodeForExecution(console.project, selection.code)
      .map { (text, range) ->
        {
          console.executeActionHandler.executeLater {
            if (isDebug && console.executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT) {
              return@executeLater resolvedPromise(false)
            }
            console.executeActionHandler.fireBeforeExecution()
            console.appendCommandText(text.trim { it <= ' ' })
            console.executeActionHandler.fireBusy()
            val newRange = TextRange(range.startOffset + selection.range.startOffset, range.endOffset + selection.range.startOffset)
            console.rInterop.replSourceFile(selection.file, textRange = newRange, debug = isDebug, firstDebugCommand = debugCommand,
                                            setLastValue = true)
              .also { debugCommand = DebugCommand.KEEP_PREVIOUS }
              .then { it.exception == null }
          }.thenAsync { it }
        }
      }
      .let { PromiseUtil.runChain(it) }
  }

  private fun executeForRMarkdownFile(project: Project, virtualFile: VirtualFile, editor: Editor, range: TextRange) {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
    RunChunkHandler.runSelectedRange(psiFile, editor, range, isDebug)
  }
}

class RunSelection : RunSelectionBase() {
  override val isDebug = false
}

class DebugSelection : RunSelectionBase() {
  override val isDebug = true
}
