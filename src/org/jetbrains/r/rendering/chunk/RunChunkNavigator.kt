/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.r.actions.RActionUtil
import org.jetbrains.r.actions.editor
import org.jetbrains.r.actions.psiFile

import java.awt.event.MouseEvent

object RunChunkNavigator : GutterIconNavigationHandler<PsiElement> {
  override fun navigate(e: MouseEvent, element: PsiElement) {
    if (isChunkRunning(element)) {
      RunChunkHandler.interruptChunkExecution(element.project)
    } else if (canRunChunk(element.project)) {
      val actions = createRunChunkActionGroup(element)
      JBPopupMenu.showByEvent(e, ActionPlaces.EDITOR_GUTTER_POPUP, actions as ActionGroup)
    }
  }

  fun createRunChunkActionsList(): List<AnAction> = actionIds.map { getAction(it) }

  private fun createRunChunkActionGroup(element: PsiElement): DefaultActionGroup =
    DefaultActionGroup(actionIds.map { id -> ChunkActionByElement(getAction(id), element) })

  private val actionIds = listOf(
    RUN_CHUNK_ACTION_ID,
    DEBUG_CHUNK_ACTION_ID,
    RUN_CHUNKS_ABOVE_ID,
    RUN_CHUNKS_BELOW_ID
  )

  private fun getAction(id: String) = ActionManager.getInstance().getAction(id)
}


internal abstract class ChunkActionBase(protected val action: AnAction): DumbAwareAction() {
  init {
    copyFrom(action)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val element = getElement(e) ?: return
    RActionUtil.performDelegatedAction(action, createActionEvent(e, element))
  }

  private fun createActionEvent(e: AnActionEvent, element: PsiElement): AnActionEvent =
    AnActionEvent.createFromInputEvent(e.inputEvent, "", null,
                                       SimpleDataContext.getSimpleContext(CODE_FENCE_DATA_KEY.name, element, e.dataContext))

  protected abstract fun getElement(e: AnActionEvent): PsiElement?
}


internal class ChunkActionByElement(action: AnAction, private val element: PsiElement) : ChunkActionBase(action) {
  override fun getElement(e: AnActionEvent) = element
}


internal class ChunkActionByOffset(action: AnAction, private val offset: Int) : ChunkActionBase(action) {

  override fun getElement(e: AnActionEvent): PsiElement? =
    e.psiFile?.viewProvider
      ?.let { it.findElementAt(offset, it.baseLanguage) }
      ?.let { it.parent.children.find { it.elementType == MarkdownTokenTypes.FENCE_LANG } }
}