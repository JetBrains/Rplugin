/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointer
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlaysManager
import org.jetbrains.r.actions.*
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.editor.ui.rMarkdownNotebook
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import java.util.concurrent.atomic.AtomicReference


fun isChunkFenceLang(element: PsiElement) =
  element.node.elementType === MarkdownTokenTypes.FENCE_LANG && element.nextSibling?.nextSibling?.node?.elementType == R_FENCE_ELEMENT_TYPE

val CODE_FENCE_DATA_KEY = DataKey.create<PsiElement>("org.jetbrains.r.rendering.chunk.actions.codeFence")
val NOTEBOOK_INTERVAL_PTR = DataKey.create<NotebookIntervalPointer>("org.jetbrains.r.rendering.chunk.actions.notebookIntervalPointer")

const val RUN_CHUNK_ACTION_ID = "org.jetbrains.r.rendering.chunk.RunChunkAction"
const val DEBUG_CHUNK_ACTION_ID = "org.jetbrains.r.rendering.chunk.DebugChunkAction"
const val RUN_CHUNKS_ABOVE_ID = "org.jetbrains.r.rendering.chunk.RunChunksAboveAction"
const val RUN_CHUNKS_BELOW_ID = "org.jetbrains.r.rendering.chunk.RunChunksBelowAction"
const val CLEAR_CHUNK_OUTPUTS_ID = "org.jetbrains.r.rendering.chunk.ClearChunkOutputsAction"

class RunChunksAboveAction: DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType && canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val offset = getStartOffset(e, editor)
    showConsoleAndRun(e) { runInRange(editor, file, 0, offset - 1) }
  }
}

class RunChunksBelowAction: DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType && canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val offset = getStartOffset(e, editor)
    showConsoleAndRun(e) { runInRange(editor, file, offset, Int.MAX_VALUE) }
  }
}

class RunChunkAction : DumbAwareAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e) }
  }
}

class DebugChunkAction : DumbAwareAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e, isDebug = true) }
  }
}

class ClearChunkOutputsAction: DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val psiElement = getCodeFenceByEvent(e) ?: return
    e.editor?.rMarkdownNotebook?.get(psiElement)?.clearOutputs(removeFiles = true)
  }
}

fun isChunkRunning(psiElement: PsiElement?) = psiElement?.let { it.project.chunkExecutionState?.currentPsiElement?.get() == it } == true

fun canRunChunk(editor: Editor?): Boolean =
  editor?.project?.let { canRunChunk(it) } == true

fun canRunChunk(project: Project): Boolean =
  RMarkdownUtil.areRequirementsSatisfied(project) && isConsoleReady(project)

private fun isConsoleReady(project: Project): Boolean {
  return RConsoleManager.getInstance(project).currentConsoleOrNull?.executeActionHandler?.let {
    it.chunkState == null && it.state == RConsoleExecuteActionHandler.State.PROMPT
  } ?: true
}

private fun getCodeFenceByEvent(e: AnActionEvent): PsiElement? {
  e.getData(CODE_FENCE_DATA_KEY)?.let { return it }
  val offset = e.intervalPointer?.get()?.let { interval -> e.editor?.document?.getLineStartOffset(interval.lines.first) }
               ?: e.caret?.offset
               ?: return null
  val file = e.psiFile ?: return null
  val markdown = SourceTreeToPsiMap.treeElementToPsi(file.node.findLeafElementAt(offset))
  val markdownCodeFence = PsiTreeUtil.getParentOfType(markdown, MarkdownCodeFence::class.java) ?: return null
  if (markdownCodeFence.children.size < 2) return null
  return markdownCodeFence.children[1].takeIf {  isChunkFenceLang(it) }
}


private fun isEnabled(e: AnActionEvent): Boolean {
  return e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == RMarkdownFileType &&
         canRunChunk(e.editor)
}

private fun runInRange(editor: Editor, file: PsiFile, startOffset: Int, endOffset: Int) {
  ChunkExecutionState(editor).apply {
    editor.chunkExecutionState = this
    RunChunkHandler.runAllChunks(file, editor, currentPsiElement, terminationRequired, startOffset - 1, endOffset).onProcessed {
      editor.chunkExecutionState = null
    }
  }
}

private fun executeChunk(e: AnActionEvent, isDebug: Boolean = false) {
  val element = getCodeFenceByEvent(e) ?: return
  val editor = e.editor as? EditorEx ?: return
  val chunkExecutionState = ChunkExecutionState(editor, currentPsiElement = AtomicReference(element), isDebug = isDebug)
  element.project.chunkExecutionState = chunkExecutionState
  RunChunkHandler.execute(element, isDebug = isDebug).onProcessed {
    element.project.chunkExecutionState = null
    val inlayElement = runReadAction { findInlayElementByFenceElement(element) } ?: return@onProcessed
    InlaysManager.getEditorManager(editor)?.updateCell(inlayElement)
  }
}

internal fun findInlayElementByFenceElement(element: PsiElement) =
  TreeUtil.findChildBackward(element.parent.node, MarkdownTokenTypes.CODE_FENCE_END)?.psi

private val AnActionEvent.codeFence: PsiElement?
  get() = getData(CODE_FENCE_DATA_KEY)

private val AnActionEvent.intervalPointer: NotebookIntervalPointer?
  get() = getData(NOTEBOOK_INTERVAL_PTR)

private fun getStartOffset(e: AnActionEvent, editor: Editor): Int =
  e.codeFence?.textRange?.startOffset
  ?: e.intervalPointer?.get()?.let { interval -> editor.document.getLineStartOffset(interval.lines.first) }
  ?: editor.caretModel.offset

private fun showConsoleAndRun(e: AnActionEvent, action: () -> Unit) {
  val editor = e.editor ?: return
  val project = e.project ?: return
  action.invoke()
  RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {
    val instance = IdeFocusManager.getInstance(project)
    instance.requestFocusInProject(instance.getFocusTargetFor(editor.component)!!, project)
  }
}