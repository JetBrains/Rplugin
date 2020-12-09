/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.datavis.r.inlays.components.GraphicsPanel
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import org.jetbrains.r.run.graphics.*
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

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

  private val plotViewer = RPlotViewer(project, parent)
  private val rootPanel = JPanel(BorderLayout())

  private val usesViewer: Boolean
    get() = isStandalone && isAutoResizeEnabled

  @Volatile
  private var oldStandalone: Boolean = true

  val preferredImageSize: Dimension
    get() = GraphicsPanel.calculateImageSizeForRegion(component.size)

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
        plotViewer.resolution = resolution
        if (usesViewer) {
          localResolution = resolution
        } else {
          scheduleRescalingIfNecessary()
        }
      }
    }

  @Volatile
  var isStandalone: Boolean = true
    set(value) {
      if (field != value && canSwitchTo(value)) {
        field = value
        updateContent()
        if (usesViewer) {
          localResolution = plotViewer.resolution
        } else {
          scheduleRescalingIfNecessary()
        }
      }
    }

  var isAutoResizeEnabled: Boolean
    get() = !graphicsPanel.isAdvancedMode
    set(value) {
      if (isAutoResizeEnabled != value) {
        graphicsPanel.isAdvancedMode = !value
        updateContent()
        if (value) {
          scheduleRescalingIfNecessary()
        } else if (isStandalone) {
          val newSize = preferredImageSize
          if (newSize.isValid) {
            graphicsPanel.showLoadingMessage(WAITING_MESSAGE)
            rescale(newSize, targetResolution)
          }
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
      plotViewer.overlayComponent = component
    }

  val hasGraphics: Boolean
    get() = snapshot != null || plot != null

  val image: BufferedImage?
    get() = if (usesViewer) plotViewer.image else graphicsPanel.image

  val maximumSize: Dimension?
    get() = graphicsPanel.maximumSize

  val component = rootPanel

  init {
    RGraphicsSettings.addDarkModeListener(project, parent) {
      rescaleIfStandalone()
    }
    project.messageBus.connect(parent).subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      rescaleIfStandalone()
    })
  }

  fun addPlot(plot: RPlot) {
    addGraphics(null, plot)
  }

  fun addSnapshot(snapshot: RSnapshot) {
    addGraphics(snapshot, null)
  }

  fun addGraphics(snapshot: RSnapshot?, plot: RPlot?) {
    if (snapshot == null && plot == null) {
      throw RuntimeException("Either snapshot or plot must be not null")
    }
    this.snapshot = snapshot
    this.plot = plot
    if (plot == null || plot.error != null) {
      isStandalone = false
    } else if (snapshot == null) {
      isStandalone = true
    }
    isAutoResizeEnabled = true
    localResolution = null
    updateContent()
    if (plot != null) {
      plotViewer.resolution = targetResolution
      plotViewer.plot = plot
    }
    if (usesViewer) {
      localResolution = targetResolution
    } else {
      graphicsPanel.showLoadingMessage(WAITING_MESSAGE)
      rescaleIfNecessary()
    }
  }

  fun addImage(file: File) {
    snapshot = null
    plot = null
    isStandalone = false
    isAutoResizeEnabled = false
    updateContent()
    graphicsPanel.showImage(file)
  }

  private fun updateContent() {
    val targetComponent = if (usesViewer) plotViewer else graphicsPanel.component
    if (rootPanel.components.firstOrNull() !== targetComponent) {
      rootPanel.removeAll()
      rootPanel.add(targetComponent, BorderLayout.CENTER)
    }
  }

  private fun canSwitchTo(newStandalone: Boolean): Boolean {
    if (newStandalone) {
      return plot != null && plot?.error == null
    } else {
      return plot == null || snapshot != null
    }
  }

  private fun showPlotError(error: RPlotError) {
    val message = RPlotUtil.getErrorDescription(error)
    if (snapshot != null) {
      graphicsPanel.showMessageWithLink(message, SWITCH_TEXT) {
        isStandalone = false
      }
    } else {
      graphicsPanel.showMessage(message)
    }
  }

  private fun scheduleRescalingIfNecessary() {
    if (usesViewer || !hasGraphics) {
      return
    }
    if (isAutoResizeEnabled || localResolution != targetResolution || oldStandalone != isStandalone) {
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
    if (usesViewer) {
      return
    }
    val oldSize = graphicsPanel.imageSize
    val newSize = preferredSize ?: preferredImageSize.takeIf { isAutoResizeEnabled } ?: oldSize
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

  private fun rescaleIfStandalone() {
    // Note: check for `localResolution` prevents useless rescales on IDE startup
    // when the first rescale hasn't been completed yet
    if (isStandalone && !isAutoResizeEnabled && localResolution != null) {
      val newSize = graphicsPanel.imageSize
      if (newSize != null && newSize.isValid) {
        rescale(newSize, targetResolution)
      }
    }
  }

  private fun rescale(newSize: Dimension, newResolution: Int?) {
    if (isStandalone) {
      plot?.let { plot ->
        if (plot.error != null) {
          showPlotError(plot.error)
        } else {
          rescale(plot, newSize, newResolution)
        }
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
      val image = RPlotUtil.createImage(plot, parameters, manager.isDarkModeEnabled, isPreview = false)
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
    private val SWITCH_TEXT = RBundle.message("plot.viewer.switch.to.builtin")

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0
  }
}
