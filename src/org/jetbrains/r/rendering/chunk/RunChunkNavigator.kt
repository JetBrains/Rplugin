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
import org.jetbrains.r.actions.RActionUtil
import org.jetbrains.r.actions.editor
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

  fun createRunChunkActionGroup(element: PsiElement): DefaultActionGroup {
    return DefaultActionGroup(
      ChunkAction(element, RUN_CHUNK_ACTION_ID),
      ChunkAction(element, DEBUG_CHUNK_ACTION_ID),
      ChunkAction(element, RUN_CHUNKS_ABOVE_ID),
      ChunkAction(element, RUN_CHUNKS_BELOW_ID)
    )
  }
}

internal class ChunkAction(private val element: PsiElement, private val action: AnAction) : DumbAwareAction() {
  init {
    copyFrom(action)
  }

  constructor(element: PsiElement, actionId: String) :
    this(element, ActionManager.getInstance().getAction(actionId))

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    RActionUtil.performDelegatedAction(action, createActionEvent(e, element))
  }


  private fun createActionEvent(e: AnActionEvent, element: PsiElement): AnActionEvent =
    AnActionEvent.createFromInputEvent(e.inputEvent, "", null,
                                       SimpleDataContext.getSimpleContext(CODE_FENCE_DATA_KEY.name, element, e.dataContext))

}