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
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
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

  val preferredImageSize: Dimension?
    get() = graphicsPanel.imageComponentSize.takeIf { it.isValid } ?: component.parent?.let { panelParent ->
      GraphicsPanel.calculateImageSizeForRegion(panelParent.preferredSize)
    }

  @Volatile
  var localResolution: Int? = null
    private set

  @Volatile
  var imagePath: String? = null
    private set

  @Volatile
  var targetResolution: Int? = null
    set(resolution) {
      if (field != resolution) {
        field = resolution
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

  fun addImage(imageFile: File, rescaleMode: RescaleMode) {
    addImage(imageFile, rescaleMode) { task ->
      task()
    }
  }

  fun <R> addImage(imageFile: File, rescaleMode: RescaleMode, executor: (() -> Unit) -> R): R {
    val path = imageFile.absolutePath
    if (rescaleMode != RescaleMode.LEFT_AS_IS) {
      isAutoResizeEnabled = manager.canRescale(path)
    }
    localResolution = manager.getImageResolution(path)
    targetResolution = localResolution  // Note: this **schedules** a rescaling as well
    imagePath = path
    return executor {
      if (rescaleMode != RescaleMode.IMMEDIATELY_RESCALE_IF_POSSIBLE || !isAutoResizeEnabled) {
        graphicsPanel.showImage(imageFile)
      } else {
        graphicsPanel.showLoadingMessage(WAITING_MESSAGE)
        rescaleIfNecessary()
      }
    }
  }

  private fun scheduleRescalingIfNecessary() {
    if (imagePath != null && (isAutoResizeEnabled || localResolution != targetResolution)) {
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
    if (newSize != null) {
      if (oldSize != newSize || localResolution != targetResolution) {
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
    imagePath?.let { path ->
      if (!manager.isBusy) {
        manager.rescaleImage(path, newSize, newResolution) { imageFile ->
          addImage(imageFile, RescaleMode.LEFT_AS_IS)
        }
      } else {
        scheduleRescaling()  // Out of luck: try again in 500 ms
      }
    }
  }

  enum class RescaleMode {
    IMMEDIATELY_RESCALE_IF_POSSIBLE,
    SCHEDULE_RESCALE_IF_POSSIBLE,
    LEFT_AS_IS,
  }

  companion object {
    private const val RESIZE_TIME_SPAN = 500
    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val WAITING_MESSAGE = "Waiting for plot to rescale"

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0
  }
}
