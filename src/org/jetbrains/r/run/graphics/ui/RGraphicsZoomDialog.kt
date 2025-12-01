/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.visualization.inlays.components.DialogUtil
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.visualization.inlays.components.BorderlessDialogWrapper
import java.awt.BorderLayout
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class RGraphicsZoomDialog(project: Project, viewerComponent: JComponent, private val zoomGroup: Disposable? = null) :
  BorderlessDialogWrapper(project, TITLE, IdeModalityType.MODELESS)
{
  private val rootPanel = JPanel(BorderLayout()).apply {
    preferredSize = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.VERY_WIDE)
    add(viewerComponent, BorderLayout.CENTER)
  }

  init {
    registerEscapeAction()
    init()
  }

  override fun createCenterPanel(): JComponent {
    return rootPanel
  }

  override fun doCancelAction() {
    super.doCancelAction()
    zoomGroup?.dispose()
  }

  private fun registerEscapeAction() {
    val listener = ActionListener { doCancelAction() }
    val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    rootPanel.registerKeyboardAction(listener, keyStroke, JComponent.WHEN_FOCUSED)
  }

  companion object {
    private val TITLE = RBundle.message("graphics.panel.zoom.dialog.title")

    fun show(project: Project, parent: Disposable, snapshot: RSnapshot) {
      val wrapper = RGraphicsPanelWrapper(project, parent)
      wrapper.targetResolution = snapshot.resolution
      val manager = ChunkGraphicsManager(project)
      var zoomGroup: Disposable? = null
      manager.createImageGroup(snapshot.file.toAbsolutePath())?.let { (copyFile, group) ->
        RSnapshot.from(copyFile)?.let { copySnapshot ->
          wrapper.addSnapshot(copySnapshot)
        }
        Disposer.register(parent, group)
        zoomGroup = group
      }
      RGraphicsZoomDialog(project, wrapper.component, zoomGroup).show()
    }

    fun show(project: Project, parent: Disposable, plot: RPlot, resolution: Int?) {
      val viewer = RPlotViewer(project, parent)
      viewer.resolution = resolution
      viewer.plot = plot
      RGraphicsZoomDialog(project, viewer).show()
    }
  }
}
