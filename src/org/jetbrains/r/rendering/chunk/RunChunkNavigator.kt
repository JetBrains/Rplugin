/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.r.actions.RActionUtil

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

  fun createChunkToolbarActionsList(psiElement: SmartPsiElementPointer<PsiElement>, editor: Editor): List<AnAction> {
    val actions = actionIds.flatMap { sublist ->
      sublist.map { id -> ChunkAction(getAction(id), psiElement, editor) } + Separator()
    }
    return actions.dropLast(1) // remove last Separator()
  }

  private fun createRunChunkActionGroup(element: PsiElement): DefaultActionGroup =
    DefaultActionGroup(actionIds[0].map { id -> ChunkAction(getAction(id), element) })

  private val actionIds = listOf(
    listOf(
      RUN_CHUNK_ACTION_ID,
      DEBUG_CHUNK_ACTION_ID,
      RUN_CHUNKS_ABOVE_ID,
      RUN_CHUNKS_BELOW_ID
    ),
    listOf(
      CLEAR_CHUNK_OUTPUTS_ID
    )
  )

  private fun getAction(id: String) = ActionManager.getInstance().getAction(id)
}


internal class ChunkAction(private val action: AnAction, private val contextVariablesOverride: Map<String, Any>): DumbAwareAction() {
  init {
    copyFrom(action)
  }

  constructor(action: AnAction, element: PsiElement): this(action, mapOf(CODE_FENCE_DATA_KEY.name to element))

  constructor(action: AnAction, elementSmartPointer: SmartPsiElementPointer<PsiElement>, editor: Editor): this(action, mapOf(
    CODE_FENCE_DATA_KEY_SMART_PTR.name to elementSmartPointer,
    CommonDataKeys.EDITOR.name to editor)
  )

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canRunChunk(CommonDataKeys.EDITOR.getData(createContext(e)))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val actionEvent = AnActionEvent.createFromInputEvent(e.inputEvent, "", null, createContext(e))
    RActionUtil.performDelegatedAction(action, actionEvent)
  }

  private fun createContext(e: AnActionEvent): DataContext =
    SimpleDataContext.getSimpleContext(contextVariablesOverride, e.dataContext)
}