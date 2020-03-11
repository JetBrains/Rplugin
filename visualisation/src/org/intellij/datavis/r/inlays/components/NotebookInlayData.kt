/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.dataframe.DataFrame
import java.awt.BorderLayout

/** Page control with table and chart pages. */
class NotebookInlayData(val project: Project, parent: Disposable, dataFrame: DataFrame) : NotebookInlayState() {
  private val inlayTablePage: InlayTablePage = InlayTablePage()
  var onChange: (() -> Unit)? = null

  private val disposable = Disposer.newDisposable()

  init {
    Disposer.register(parent, disposable)
    layout = BorderLayout()
    add(inlayTablePage, DEFAULT_LAYER)
    inlayTablePage.onChange = { onChange?.invoke() }
    MouseWheelUtils.wrapMouseWheelListeners(inlayTablePage.scrollPane, parent)

    inlayTablePage.setDataFrame(dataFrame)
  }

  private fun createClearAction(): DumbAwareAction {
    return object : DumbAwareAction(VisualizationBundle.message("notebook.inlay.clear.text"),
                                    VisualizationBundle.message("notebook.inlay.clear.description"),
                                    AllIcons.Actions.GC) {
      override fun actionPerformed(e: AnActionEvent) {
        clearAction.invoke()
      }
    }
  }

  private fun createActionButton(action: AnAction) :ActionButton {
    return ActionButton(action, action.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }


  fun setDataFrame(dataFrame: DataFrame) {
    inlayTablePage.setDataFrame(dataFrame)

    onHeightCalculated?.invoke(inlayTablePage.preferredHeight)
  }

  override fun clear() {
  }

  override fun getCollapsedDescription(): String {
    return "no description"
  }
}