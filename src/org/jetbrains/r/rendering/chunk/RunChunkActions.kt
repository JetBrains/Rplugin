/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.actionSystem.ActionManager.getInstance
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.getCell
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.editor
import org.jetbrains.r.actions.psiFile
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.editor.ui.rMarkdownCellToolbarPanel
import org.jetbrains.r.editor.ui.rMarkdownNotebook
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import java.util.concurrent.atomic.AtomicReference


object RunChunkActions {
  fun createToolbarActionGroup(): DefaultActionGroup =
    getInstance().getAction("org.jetbrains.r.rendering.chunk.toolbar") as DefaultActionGroup
}


fun isChunkFenceLang(element: PsiElement) =
  element.node.elementType === MarkdownTokenTypes.FENCE_LANG && element.nextSibling?.nextSibling?.node?.elementType == R_FENCE_ELEMENT_TYPE

private abstract class BaseChunkAction : DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class RunChunksAboveAction : BaseChunkAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val interval = getCurrentInterval(e, editor)
    val endOffset = getStartOffset(editor, interval) - 1
    showConsoleAndRun(e) {
      RunChunkHandler.runAllChunks(file, editor, 0, endOffset)
    }
  }
}

private class RunChunksBelowAction : BaseChunkAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val interval = getCurrentInterval(e, editor)
    val startOffset = getStartOffset(editor, interval)
    showConsoleAndRun(e) {
      RunChunkHandler.runAllChunks(file, editor, startOffset - 1, Int.MAX_VALUE)
    }
  }
}

private class RunChunkAction : BaseChunkAction() {
  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e) }
  }
}

private class DebugChunkAction : BaseChunkAction() {
  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e, isDebug = true) }
  }
}

private class ClearChunkOutputsAction : BaseChunkAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val interval = getCurrentInterval(e, editor)
    e.editor?.rMarkdownNotebook?.get(interval)?.clearOutputs(removeFiles = true)
  }
}

fun canRunChunk(editor: Editor?): Boolean =
  editor?.project?.let { canRunChunk(it) } == true

fun canRunChunk(project: Project): Boolean =
  RMarkdownUtil.areRequirementsSatisfied(project) && isConsoleReady(project)

private fun isConsoleReady(project: Project): Boolean {
  return RConsoleManager.getInstance(project).currentConsoleOrNull?.executeActionHandler?.let {
    it.chunkState == null && it.state == RConsoleExecuteActionHandler.State.PROMPT
  } ?: true
}

private fun getStartOffset(editor: Editor, interval: NotebookCellLines.Interval): Int =
  editor.document.getLineStartOffset(interval.lines.first)

private fun getCodeFenceByEvent(e: AnActionEvent, editor: Editor): PsiElement? {
  val interval = getCurrentInterval(e, editor)
  val offset = getStartOffset(editor, interval)
  val file = e.psiFile ?: return null
  val markdown = SourceTreeToPsiMap.treeElementToPsi(file.node.findLeafElementAt(offset))
  val markdownCodeFence = PsiTreeUtil.getParentOfType(markdown, MarkdownCodeFence::class.java) ?: return null
  if (markdownCodeFence.children.size < 2) return null
  return markdownCodeFence.children[1].takeIf { isChunkFenceLang(it) }
}


private fun isEnabled(e: AnActionEvent): Boolean {
  return e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == RMarkdownFileType &&
         canRunChunk(e.editor)
}

private fun executeChunk(e: AnActionEvent, isDebug: Boolean = false) {
  val editor = e.editor as? EditorEx ?: return
  val element = getCodeFenceByEvent(e, editor) ?: return
  val chunkExecutionState = ChunkExecutionState(editor, currentPsiElement = AtomicReference(element), isDebug = isDebug)
  element.project.chunkExecutionState = chunkExecutionState
  RunChunkHandler.execute(element, isDebug = isDebug).onProcessed {
    element.project.chunkExecutionState = null
    /** outputs will be updated in [RunChunkHandler.afterRunChunk] */
  }
}

private fun getCurrentInterval(e: AnActionEvent, editor: Editor): NotebookCellLines.Interval =
  e.rMarkdownCellToolbarPanel?.pointer?.get()
  ?: editor.getCell(editor.document.getLineNumber(editor.caretModel.offset))

private fun showConsoleAndRun(e: AnActionEvent, action: () -> Unit) {
  val editor = e.editor ?: return
  val project = e.project ?: return
  action.invoke()
  RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {
    val instance = IdeFocusManager.getInstance(project)
    instance.requestFocusInProject(instance.getFocusTargetFor(editor.component)!!, project)
  }
}