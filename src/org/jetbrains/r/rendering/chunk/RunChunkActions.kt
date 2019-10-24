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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.util.PsiTreeUtil
import icons.org.jetbrains.r.RBundle
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl
import org.jetbrains.r.actions.*
import org.jetbrains.r.rendering.editor.RunAllState
import org.jetbrains.r.rendering.editor.runAllState
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE


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
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType && !isRunningAllChunks(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val offset = e.codeFence?.textRange?.startOffset ?: editor.caretModel.offset
    runInRange(editor, file, 0, offset - 1)
  }
}

class RunChunksBelowAction: AbstractRunChunksBelowAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType && !isRunningAllChunks(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val file = e.psiFile ?: return
    val offset = e.codeFence?.textRange?.startOffset ?: editor.caretModel.offset
    runInRange(editor, file, offset, Int.MAX_VALUE)
  }
}

class RunChunkAction : AbstractRunChunkAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    RunChunkHandler.execute(getCodeFenceByEvent(e)!!)
  }
}

class DebugChunkAction : AbstractDebugChunkAction(), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isEnabled(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    RunChunkHandler.execute(getCodeFenceByEvent(e)!!, debug = true)
  }
}

class InterruptChunkExecutionAction :
  AnAction(RBundle.message("run.chunk.action.interrupt.text"), null, AllIcons.Actions.Suspend), RPromotedAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.virtualFile?.fileType == RMarkdownFileType &&
                               getCodeFenceByEvent(e)?.fenceLangChunkState?.isNotNone == true
  }

  override fun actionPerformed(e: AnActionEvent) {
    RunChunkHandler.interruptChunkExecution(getCodeFenceByEvent(e)!!)
  }
}

fun isRunningAllChunks(editor: Editor?): Boolean = editor?.runAllState != null

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
         !isRunningAllChunks(e.editor) &&
         getCodeFenceByEvent(e)?.let { it.fenceLangChunkState.let { s -> s == null || s == ChunkState.NONE} } == true
}

private fun runInRange(editor: Editor, file: PsiFile, startOffset: Int, endOffset: Int) {
  RunAllState().apply {
    editor.runAllState = this
    RunChunkHandler.runAllChunks(file, currentPsiElement, terminationRequired, startOffset - 1, endOffset).onProcessed {
      editor.runAllState = null
    }
  }
}

private val AnActionEvent.codeFence: PsiElement?
  get() = getData(CODE_FENCE_DATA_KEY)