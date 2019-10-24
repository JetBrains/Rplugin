/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.util.PsiTreeUtil
import icons.org.jetbrains.r.RBundle
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl
import org.jetbrains.r.actions.*
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
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

abstract class AbstractRunChunksAboveAction : AnAction(RBundle.message("run.chunk.action.runAbove.text"), null,
                                                       AllIcons.Actions.RunToCursor)

abstract class AbstractRunChunksBelowAction: AnAction(RBundle.message("run.chunk.action.runBelow.text"), null,
                                                      AllIcons.Actions.Rerun)

abstract class AbstractRunChunkAction : AnAction(RBundle.message("run.chunk.action.run.text"), null,
                                                 AllIcons.Actions.Execute)

abstract class AbstractDebugChunkAction : AnAction(RBundle.message("run.chunk.action.debug.text"), null,
                                                   AllIcons.Actions.StartDebugger)

class RunChunksAboveAction: AbstractRunChunksAboveAction(), RPromotedAction {
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

class RunChunksBelowAction: AbstractRunChunksBelowAction(), RPromotedAction {
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

class RunChunkAction : AbstractRunChunkAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e) }
  }
}

class DebugChunkAction : AbstractDebugChunkAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    showConsoleAndRun(e) { executeChunk(e, isDebug = true) }
  }
}

class InterruptChunkExecutionAction :
  AnAction(RBundle.message("run.chunk.action.interrupt.text"), null, AllIcons.Actions.Suspend), RPromotedAction {

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
  RConsoleManager.getInstance(project).currentConsoleOrNull?.executeActionHandler?.let {
    it.chunkState == null && it.state == RConsoleExecuteActionHandler.State.PROMPT
  } ?: true

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
  ChunkExecutionState().apply {
    editor.chunkExecutionState = this
    RunChunkHandler.runAllChunks(file, currentPsiElement, terminationRequired, startOffset - 1, endOffset).onProcessed {
      editor.chunkExecutionState = null
    }
  }
}

private fun executeChunk(e: AnActionEvent, isDebug: Boolean = false) {
  val element = getCodeFenceByEvent(e)!!
  element.project.chunkExecutionState = ChunkExecutionState(currentPsiElement = AtomicReference(element), isDebug = isDebug)
  RunChunkHandler.execute(element, isDebug = isDebug).onProcessed { element.project.chunkExecutionState = null }
}

private val AnActionEvent.codeFence: PsiElement?
  get() = getData(CODE_FENCE_DATA_KEY)

private fun showConsoleAndRun(e: AnActionEvent, action: () -> Unit) {
  val editor = e.editor ?: return
  val project = e.project ?: return
  RConsoleToolWindowFactory.getToolWindow(project)?.show {
    action.invoke()
    val instance = IdeFocusManager.getInstance(project)
    instance.requestFocusInProject(instance.getFocusTargetFor(editor.component)!!, project)
  }
}