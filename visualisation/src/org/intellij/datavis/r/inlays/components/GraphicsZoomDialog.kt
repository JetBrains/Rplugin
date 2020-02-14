/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class GraphicsZoomDialog(project: Project, parent: Disposable, imagePath: String) :
  DialogWrapper(project, null, true, IdeModalityType.MODELESS, false)
{
  private val graphicsManager = GraphicsManager.getInstance(project)
  private val wrapper = GraphicsPanelWrapper(project, parent)

  private val rootPanel = JPanel(BorderLayout()).apply {
    preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.VERY_WIDE)
    add(wrapper.component, BorderLayout.CENTER)
  }

  private var zoomGroup: Disposable? = null

  init {
    init()
    title = TITLE
    removeMarginsIfPossible()
    graphicsManager?.createImageGroup(imagePath)?.let { pair ->
      wrapper.addImage(pair.first, true)
      Disposer.register(parent, pair.second)
      zoomGroup = pair.second
    }
  }

  override fun createCenterPanel(): JComponent? {
    return rootPanel
  }

  override fun doCancelAction() {
    super.doCancelAction()
    zoomGroup?.dispose()
  }

  private fun removeMarginsIfPossible() {
    (rootPane.contentPane as JPanel?)?.let { panel ->
      panel.border = JBUI.Borders.empty()
    }
  }

  companion object {
    private const val TITLE = "Graphics output"
  }
}
