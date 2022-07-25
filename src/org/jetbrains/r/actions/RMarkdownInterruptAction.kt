package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState

class RMarkdownInterruptAction: DumbAwareAction(), RPromotedAction {
  override fun actionPerformed(e: AnActionEvent) {
    val state = e.editor?.chunkExecutionState ?: return
    interruptChunkExecution(state)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.editor?.chunkExecutionState != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    fun interruptChunkExecution(state: ChunkExecutionState) {
      state.terminationRequired.set(true)
      val element = state.currentPsiElement.get() ?: return
      RunChunkHandler.interruptChunkExecution(element.project)
    }
  }
}