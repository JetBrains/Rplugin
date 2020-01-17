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
import org.intellij.datavis.inlays.InlaysManager
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl
import org.jetbrains.r.actions.*
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import java.util.concurrent.atomic.AtomicReference


fun isChunkFenceLang(element: PsiElement) =
  element.node.elementType === MarkdownTokenTypes.FENCE_LANG && element.nextSibling?.nextSibling?.node?.elementType == R_FENCE_ELEMENT_TYPE

val CODE_FENCE_DATA_KEY = DataKey.create<PsiElement>("org.jetbrains.r.rendering.chunk.actions.codeFence")

const val RUN_CHUNK_ACTION_ID = "org.jetbrains.r.rendering.chunk.RunChunkAction"
const val DEBUG_CHUNK_ACTION_ID = "org.jetbrains.r.rendering.chunk.DebugChunkAction"
const val RUN_CHUNKS_ABOVE_ID = "org.jetbrains.r.rendering.chunk.RunChunksAboveAction"
const val RUN_CHUNKS_BELOW_ID = "org.jetbrains.r.rendering.chunk.RunChunksBelowAction"
const val INTERRUPT_CHUNK_EXECUTION_ID = "org.jetbrains.r.rendering.chunk.InterruptChunkExecutionAction"

class RunChunksAboveAction: DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType && canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val offset = e.codeFence?.textRange?.startOffset ?: editor.caretModel.offset
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
    val offset = e.codeFence?.textRange?.startOffset ?: editor.caretModel.offset
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

class InterruptChunkExecutionAction : DumbAwareAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType &&
                               e.project?.chunkExecutionState?.let { getCodeFenceByEvent(e) == it.currentPsiElement.get() } == true
  }

  override fun actionPerformed(e: AnActionEvent) {
    RunChunkHandler.interruptChunkExecution(e.project!!)
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
  val offset = e.caret?.offset ?: return null
  val file = e.psiFile ?: return null
  val markdown = SourceTreeToPsiMap.treeElementToPsi(file.node.findLeafElementAt(offset))
  val markdownCodeFence = PsiTreeUtil.getParentOfType(markdown, MarkdownCodeFenceImpl::class.java) ?: return null
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
  val element = getCodeFenceByEvent(e)!!
  val parent = element.parent ?: return
  val editor = e.editor as? EditorEx ?: return
  val document = editor.document
  val chunkExecutionState = ChunkExecutionState(editor, currentPsiElement = AtomicReference(element), isDebug = isDebug)
  val range = IntRange(document.getLineNumber(parent.textRange.startOffset), document.getLineNumber(parent.textRange.endOffset))
  chunkExecutionState.pendingLineRanges.add(range)
  chunkExecutionState.currentLineRange = null
  chunkExecutionState.revalidateGutter()
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

private fun showConsoleAndRun(e: AnActionEvent, action: () -> Unit) {
  val editor = e.editor ?: return
  val project = e.project ?: return
  action.invoke()
  RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {
    val instance = IdeFocusManager.getInstance(project)
    instance.requestFocusInProject(instance.getFocusTargetFor(editor.component)!!, project)
  }
}