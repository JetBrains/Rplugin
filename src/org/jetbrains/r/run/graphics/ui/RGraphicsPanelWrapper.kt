/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.datavis.r.inlays.components.GraphicsPanel
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.run.graphics.RSnapshot
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JComponent

class RGraphicsPanelWrapper(project: Project, private val parent: Disposable) {
  private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)
  private val manager = ChunkGraphicsManager(project)

  private val graphicsPanel = GraphicsPanel(project, parent).apply {
    component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        scheduleRescalingIfNecessary()
      }
    })
    showLoadingMessage()
  }

  @Volatile
  private var oldStandalone: Boolean = true

  val preferredImageSize: Dimension?
    get() = graphicsPanel.imageComponentSize.takeIf { it.isValid } ?: component.parent?.let { panelParent ->
      GraphicsPanel.calculateImageSizeForRegion(panelParent.preferredSize)
    }

  @Volatile
  var localResolution: Int? = null
    private set

  @Volatile
  var snapshot: RSnapshot? = null
    private set

  @Volatile
  var plot: RPlot? = null
    private set

  @Volatile
  var targetResolution: Int? = null
    set(resolution) {
      if (field != resolution) {
        field = resolution
        scheduleRescalingIfNecessary()
      }
    }

  @Volatile
  var isStandalone: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        scheduleRescalingIfNecessary()
      }
    }

  var isAutoResizeEnabled: Boolean
    get() = !graphicsPanel.isAdvancedMode
    set(value) {
      if (isAutoResizeEnabled != value) {
        graphicsPanel.isAdvancedMode = !value
        if (value) {
          scheduleRescalingIfNecessary()
        }
      }
    }

  @Volatile
  var isVisible: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        if (value) {
          scheduleRescalingIfNecessary()
        }
      }
    }

  var overlayComponent: JComponent?
    get() = graphicsPanel.overlayComponent
    set(component) {
      graphicsPanel.overlayComponent = component
    }

  val image: BufferedImage?
    get() = graphicsPanel.image

  val maximumHeight: Int?
    get() = graphicsPanel.maximumSize?.height

  val maximumWidth: Int?
    get() = graphicsPanel.maximumSize?.width

  val component = graphicsPanel.component

  fun addGraphics(snapshot: RSnapshot, plot: RPlot? = null) {
    this.snapshot = snapshot
    this.plot = plot
    if (plot == null) {
      isStandalone = false
    }
    isAutoResizeEnabled = true
    localResolution = null
    graphicsPanel.showLoadingMessage(WAITING_MESSAGE)
    rescaleIfNecessary()
  }

  fun addImage(file: File) {
    snapshot = null
    plot = null
    graphicsPanel.showImage(file)
  }

  private fun scheduleRescalingIfNecessary() {
    if (snapshot != null && (isAutoResizeEnabled || localResolution != targetResolution)) {
      scheduleRescaling()
    }
  }

  private fun scheduleRescaling() {
    queue.queue(object : Update(RESIZE_TASK_IDENTITY) {
      override fun run() {
        rescaleIfNecessary()
      }
    })
  }

  fun rescaleIfNecessary(preferredSize: Dimension? = null) {
    val oldSize = graphicsPanel.imageSize
    val newSize = preferredSize ?: preferredImageSize?.takeIf { isAutoResizeEnabled } ?: oldSize
    if (newSize != null && newSize.isValid) {
      if (oldSize != newSize || localResolution != targetResolution || oldStandalone != isStandalone) {
        // Note: there might be lots of attempts to resize image on IDE startup
        // but most of them will fail (and throw an exception)
        // due to the parent being disposed
        if (!Disposer.isDisposed(parent) && isVisible) {
          rescale(newSize, targetResolution)
        }
      }
    }
  }

  private fun rescale(newSize: Dimension, newResolution: Int?) {
    if (isStandalone) {
      plot?.let { plot ->
        rescale(plot, newSize, newResolution)
      }
    } else {
      snapshot?.let { snapshot ->
        rescale(snapshot, newSize, newResolution)
      }
    }
  }

  private fun rescale(plot: RPlot, newSize: Dimension, newResolution: Int?) {
    runAsync {
      val parameters = RGraphicsUtils.ScreenParameters(newSize, newResolution)
      val image = RPlotUtil.createImage(plot, parameters)
      localResolution = newResolution
      oldStandalone = isStandalone
      graphicsPanel.showBufferedImage(image)
      scheduleRescalingIfNecessary()
    }
  }

  private fun rescale(snapshot: RSnapshot, newSize: Dimension, newResolution: Int?) {
    if (!manager.isBusy) {
      manager.rescaleImage(snapshot.file.absolutePath, newSize, newResolution) { imageFile ->
        localResolution = manager.getImageResolution(imageFile.absolutePath)
        this.snapshot = RSnapshot.from(imageFile)
        oldStandalone = isStandalone
        graphicsPanel.showImage(imageFile)
        scheduleRescalingIfNecessary()
      }
    } else {
      scheduleRescaling()  // Out of luck: try again in 500 ms
    }
  }

  companion object {
    private const val RESIZE_TIME_SPAN = 500
    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private val WAITING_MESSAGE = RBundle.message("graphics.panel.wrapper.waiting")

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0
  }
}
