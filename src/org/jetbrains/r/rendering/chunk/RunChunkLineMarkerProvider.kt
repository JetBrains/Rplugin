/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import org.jetbrains.r.actions.editor
import java.awt.event.MouseEvent
import javax.swing.Icon

class RunChunkLineMarkerProvider : LineMarkerProvider{

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
    if (isChunkFenceLang(element)) {
      return RunChunkMarkerInfo(element)
    }
    return null
  }

  class RunChunkMarkerInfo(element: PsiElement) :
    LineMarkerInfo<PsiElement>(element, element.textRange, null, null, RunChunkNavigator, GutterIconRenderer.Alignment.RIGHT) {

    override fun createGutterRenderer(): GutterIconRenderer? {
      return RunChunkGutterIconRenderer()
    }

    inner class RunChunkGutterIconRenderer : LineMarkerGutterIconRenderer<PsiElement>(this@RunChunkMarkerInfo) {
      override fun getIcon(): Icon =
        when {
          isChunkRunning(element) -> AllIcons.Actions.Suspend
          element?.let { !canRunChunk(it.project) } == true -> IconLoader.getDisabledIcon(AllIcons.Actions.Execute)
          else -> AllIcons.Actions.Execute
        }
    }
  }
}

object RunChunkNavigator : GutterIconNavigationHandler<PsiElement> {
  override fun navigate(e: MouseEvent, element: PsiElement) {
    val actionManager = ActionManager.getInstance()
    if (isChunkRunning(element)) {
      RunChunkHandler.interruptChunkExecution(element.project)
    } else if (canRunChunk(element.project)) {
      val actions = createRunChunkActionGroup(element)
      actionManager.createActionPopupMenu(ActionPlaces.EDITOR_GUTTER_POPUP, actions as ActionGroup)
        .getComponent()
        .show(e.component, e.getX(), e.getY())
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

internal class ChunkAction(private val element: PsiElement, private val action: AnAction) : AnAction() {
  init {
    copyFrom(action)
  }

  constructor(element: PsiElement, actionId: String) :
    this(element, ActionManager.getInstance().getAction(actionId))

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = canRunChunk(e.editor)
  }

  override fun actionPerformed(e: AnActionEvent) {
    action.actionPerformed(createActionEvent(e, element))
  }


  private fun createActionEvent(e: AnActionEvent, element: PsiElement): AnActionEvent =
    AnActionEvent.createFromInputEvent(e.inputEvent, "", null,
                                       SimpleDataContext.getSimpleContext(CODE_FENCE_DATA_KEY.name, element, e.dataContext))

}