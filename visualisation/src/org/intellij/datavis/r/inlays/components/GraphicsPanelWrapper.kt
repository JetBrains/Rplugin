/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File

class GraphicsPanelWrapper(project: Project, private val parent: Disposable) {
  private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)
  private val graphicsManager = GraphicsManager.getInstance(project)

  private val graphicsPanel = GraphicsPanel(project, parent).apply {
    component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        scheduleRescalingIfNecessary()
      }
    })
  }

  private val preferredImageSize: Dimension?
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

  @Volatile
  var isAutoResizeEnabled: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        graphicsPanel.isAdvancedMode = !isAutoResizeEnabled
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

  val maximumHeight: Int?
    get() = graphicsPanel.maximumSize?.height

  val component = graphicsPanel.component

  fun addImage(imageFile: File, immediatelyRescale: Boolean) {
    addImage(imageFile, immediatelyRescale) { task ->
      task()
    }
  }

  fun <R> addImage(imageFile: File, immediatelyRescale: Boolean, executor: (() -> Unit) -> R): R {
    val path = imageFile.absolutePath
    localResolution = graphicsManager?.getImageResolution(path)
    targetResolution = localResolution  // Note: this **schedules** a rescaling as well
    imagePath = path
    return executor {
      if (!immediatelyRescale) {
        graphicsPanel.showImage(imageFile)
      } else {
        graphicsPanel.showMessage(WAITING_MESSAGE)
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

  fun showSvgImage(data: String) {
    graphicsPanel.showSvgImage(data)
  }

  fun showImageBase64(data: String) {
    graphicsPanel.showImageBase64(data)
  }

  private fun rescaleIfNecessary() {
    preferredImageSize?.let { newSize ->
      val oldSize = graphicsPanel.imageSize
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
      graphicsManager?.let { manager ->
        if (!manager.isBusy) {
          manager.rescaleImage(path, newSize, newResolution) { imageFile ->
            addImage(imageFile, false)
          }
        } else {
          scheduleRescaling()  // Out of luck: try again in 500 ms
        }
      }
    }
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
