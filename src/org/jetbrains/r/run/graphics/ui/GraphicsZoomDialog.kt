/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.inlays.components.BorderlessDialogWrapper
import org.intellij.datavis.r.inlays.components.DialogUtil
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class GraphicsZoomDialog(project: Project, parent: Disposable, imagePath: String) :
  BorderlessDialogWrapper(project, TITLE, IdeModalityType.MODELESS)
{
  private val manager = ChunkGraphicsManager(project)
  private val wrapper = GraphicsPanelWrapper(project, parent)

  private val rootPanel = JPanel(BorderLayout()).apply {
    preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.VERY_WIDE)
    add(wrapper.component, BorderLayout.CENTER)
  }

  private var zoomGroup: Disposable? = null

  init {
    init()
    manager.createImageGroup(imagePath)?.let { pair ->
      wrapper.addImage(pair.first, GraphicsPanelWrapper.RescaleMode.IMMEDIATELY_RESCALE_IF_POSSIBLE)
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

  companion object {
    private val TITLE = VisualizationBundle.message("inlay.output.image.zoom.dialog.title")
  }
}
