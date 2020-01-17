// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.intellij.datavis.inlays.components.CHANGE_DARK_MODE_TOPIC
import org.intellij.datavis.inlays.components.GraphicsPanel
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.run.graphics.RSnapshotsUpdate
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class RGraphicsToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
  private var lastNormal = listOf<RSnapshot>()
  private var lastZoomed = listOf<RSnapshot>()
  private var lastIndex = -1

  private val lastSnapshot: RSnapshot?
    get() = if (lastIndex in lastNormal.indices) lastNormal[lastIndex] else null

  private val lastNumber: Int?
    get() = lastSnapshot?.number

  private val lastFile: File?
    get() = lastSnapshot?.file

  private var isAutoResizeEnabled: Boolean = true

  private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)
  private val repository = RGraphicsRepository.getInstance(project)
  private val graphicsPanel = GraphicsPanel(project, project)

  init {
    setContent(graphicsPanel.component)
    val groups = createActionHolderGroups(project)
    toolbar = RGraphicsToolbar(groups).component
    project.messageBus.syncPublisher(CHANGE_DARK_MODE_TOPIC).onDarkModeChanged(RGraphicsSettings.isDarkModeEnabled(project))

    graphicsPanel.component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        if (isAutoResizeEnabled) {
          queue.queue(object : Update(RESIZE_TASK_IDENTITY) {
            override fun run() {
              postScreenDimension()
            }
          })
        }
      }
    })

    repository.addSnapshotListener { snapshotUpdate ->
      ApplicationManager.getApplication().invokeLater {
        refresh(snapshotUpdate)
      }
    }
  }

  private fun refresh(update: RSnapshotsUpdate) {
    val normal = update.normal
    if (normal.isNotEmpty()) {
      val loadedNumber = loadSnapshotNumber()
      lastIndex = if (loadedNumber != null) {
        val suggested = normal.indexOfFirst { it.number == loadedNumber }
        if (suggested >= 0) {
          suggested
        } else {
          normal.lastIndex
        }
      } else {
        normal.lastIndex
      }
      lastNormal = normal
      lastZoomed = update.zoomed
      showCurrent()
    } else {
      reset()
    }
    postSnapshotNumber()
    if (isAutoResizeEnabled) {
      postScreenDimension()
    }
  }

  private fun reset() {
    lastNormal = listOf()
    lastZoomed = listOf()
    lastIndex = -1
    graphicsPanel.reset()
  }

  private fun showCurrent() {
    lastSnapshot?.let { snapshot ->
      val message = snapshot.error
      if (message != null) {
        graphicsPanel.showMessage(message)
      } else {
        graphicsPanel.showImage(snapshot.file)
      }
    }
  }

  private fun createActionHolderGroups(project: Project): List<RGraphicsToolbar.ActionHolderGroup> {
    class PreviousGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = PREVIOUS_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastIndex > 0

      override fun onClick() {
        lastIndex--
        showCurrent()
        postSnapshotNumber()
      }
    }

    class NextGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = NEXT_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastIndex < lastNormal.lastIndex

      override fun onClick() {
        lastIndex++
        showCurrent()
        postSnapshotNumber()
      }
    }

    class ExportGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = EXPORT_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty() && lastSnapshot?.error == null

      override fun onClick() {
        val title = RBundle.message("graphics.panel.file.saver.title")
        val description = RBundle.message("graphics.panel.file.saver.description")
        val descriptor = FileSaverDescriptor(title, description, "png")
        val baseFile = VfsUtil.findFile(Paths.get(project.basePath!!), false)
        val wrapper = FileSaverDialogImpl(descriptor, project).save(baseFile, "Snapshot")
        if (wrapper != null) {
          try {
            val destination = wrapper.file
            createDestinationFile(destination)
            lastFile?.let { source ->
              Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
          } catch (e: Exception) {
            val details = e.message?.let { ".\n$it" } ?: ""
            val header = RBundle.message("graphics.panel.file.saver.failure.header")
            RNotificationUtil.notifyGraphicsError(project, "$header: ${wrapper.file}$details")
          }
        }
      }
    }

    class ZoomGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = ZOOM_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        val snapshot = lastZoomed[lastIndex]
        Desktop.getDesktop().open(snapshot.file)
      }
    }

    class ClearGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = CLEAR_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        lastNumber?.let { number ->
          repository.clearSnapshot(number)
        }
      }
    }

    class ClearAllGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = CLEAR_ALL_GRAPHICS_ACTION_ID

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        repository.clearAllSnapshots()
      }
    }

    class TuneGraphicsDeviceActionHolder : RGraphicsToolbar.ActionHolder {
      override val id = TUNE_GRAPHICS_DEVICE_ACTION_ID

      override val canClick: Boolean
        get() = true

      override fun onClick() {
        val panelDimension = getAdjustedScreenDimension()
        val initialParameters = getInitialParameters()
        RGraphicsSettingsDialog(panelDimension, initialParameters, isAutoResizeEnabled) { parameters, isEnabled ->
          // Note: if auto resize is enabled, you can assume here that
          // `parameters.dimension` equals to current panel size
          // so there is no need to call `postScreenDimension()`
          RGraphicsSettings.setScreenParameters(project, parameters)
          graphicsPanel.isAdvancedMode = !isEnabled
          isAutoResizeEnabled = isEnabled
          repository.apply {
            configuration?.let { oldConfiguration ->
              configuration = oldConfiguration.copy(screenParameters = parameters)
            }
          }
        }.show()
      }

      private fun getInitialParameters(): RGraphicsUtils.ScreenParameters {
        return repository.configuration?.screenParameters ?: RGraphicsSettings.getScreenParameters(project)
      }
    }

    return listOf(
      RGraphicsToolbar.groupOf(
        PreviousGraphicsActionHolder(),
        NextGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ExportGraphicsActionHolder(),
        ZoomGraphicsActionHolder(),
        ClearGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ClearAllGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        TuneGraphicsDeviceActionHolder()
      )
    )
  }

  private fun getAdjustedScreenDimension(): Dimension {
    return graphicsPanel.imageComponentSize
  }

  private fun postScreenDimension() {
    val newDimension = getAdjustedScreenDimension()
    if (newDimension.width > 0 && newDimension.height > 0) {
      RGraphicsSettings.setScreenDimension(project, newDimension)
      repository.configuration?.let { oldConfiguration ->
        val parameters = oldConfiguration.screenParameters
        val newParameters = parameters.copy(dimension = newDimension)
        repository.configuration = oldConfiguration.copy(screenParameters = newParameters)
      }
    }
  }

  private fun postSnapshotNumber() {
    repository.configuration?.let { oldConfiguration ->
      repository.configuration = oldConfiguration.copy(snapshotNumber = lastNumber)
    }
  }

  private fun loadSnapshotNumber(): Int? {
    return repository.configuration?.snapshotNumber
  }

  companion object {
    const val TOOL_WINDOW_ID = "R Graphics"

    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val RESIZE_TIME_SPAN = 500

    private const val PREVIOUS_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RPreviousGraphicsAction"
    private const val NEXT_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RNextGraphicsAction"
    private const val EXPORT_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RExportGraphicsAction"
    private const val ZOOM_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RZoomGraphicsAction"
    private const val CLEAR_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RClearGraphicsAction"
    private const val CLEAR_ALL_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RClearAllGraphicsAction"
    private const val TUNE_GRAPHICS_DEVICE_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RTuneGraphicsDeviceAction"

    private fun createDestinationFile(file: File) {
      if (!file.exists() && !file.createNewFile()) {
        throw RuntimeException(RBundle.message("graphics.panel.file.saver.failure.details"))
      }
    }
  }
}