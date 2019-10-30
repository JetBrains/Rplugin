// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.datavis.inlays.components.GraphicsPanel
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.run.graphics.*
import java.awt.Desktop
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.swing.JSplitPane

class RGraphicsToolWindow(project: Project) : SimpleToolWindowPanel(true, true) {
  private var lastNormal = listOf<RSnapshot>()
  private var lastZoomed = listOf<RSnapshot>()
  private var lastIndex = -1

  private val lastSnapshot: RSnapshot?
    get() = if (lastIndex in lastNormal.indices) lastNormal[lastIndex] else null

  private val lastNumber: Int?
    get() = lastSnapshot?.number

  private val lastFile: File?
    get() = lastSnapshot?.file

  private val isAutoResizeEnabled: Boolean
    get() = settingsSubDialog.isAutoResizeEnabled

  private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)
  private val repository = RGraphicsRepository.getInstance(project)
  private val graphicsPanel = GraphicsPanel(project)

  private val settingsSubDialog = RGraphicsSettingsDialog(object : RGraphicsSettingsDialog.Listener {
    override fun onParametersChange(parameters: RGraphicsUtils.ScreenParameters) {
      RGraphicsSettings.setScreenParameters(project, parameters)
      repository.apply {
        configuration?.let { oldConfiguration ->
          configuration = oldConfiguration.copy(screenParameters = parameters)
          if (oldConfiguration.screenParameters.resolution != parameters.resolution) {
            Messages.showInfoMessage(RESOLUTION_CHANGED_MESSAGE, RESOLUTION_CHANGED_TITLE)
          }
        }
      }
    }

    override fun onAutoResizeSwitch(isEnabled: Boolean) {
      if (isEnabled) {
        postScreenDimension()
      }
    }
  })

  private val settingsScrollable = JBScrollPane(settingsSubDialog.createComponent())
  private val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, true, graphicsPanel.component, settingsScrollable).apply {
    resizeWeight = RESIZE_SPLIT_WEIGHT
  }

  init {
    setContent(splitPane)
    val groups = createActionHolderGroups(project)
    toolbar = RGraphicsToolbar(groups).component

    graphicsPanel.component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        if (isAutoResizeEnabled) {
          settingsSubDialog.apply {
            currentParameters?.let { oldParameters ->
              currentParameters = oldParameters.copy(dimension = getAdjustedScreenDimension())
            }
          }
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
      when (snapshot.error) {
        RSnapshotError.MARGIN -> graphicsPanel.showMessage(MARGINS_TOO_LARGE_MESSAGE)
        else -> graphicsPanel.refresh(snapshot.file)
      }
    }
  }

  private fun createActionHolderGroups(project: Project): List<RGraphicsToolbar.ActionHolderGroup> {
    class PreviousGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val title: String? = null
      override val description = PREVIOUS_GRAPHICS_ACTION_DESCRIPTION
      override val icon = PREVIOUS_GRAPHICS_ACTION_ICON

      override val canClick: Boolean
        get() = lastIndex > 0

      override fun onClick() {
        lastIndex--
        showCurrent()
        postSnapshotNumber()
      }
    }

    class NextGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val title: String? = null
      override val description = NEXT_GRAPHICS_ACTION_DESCRIPTION
      override val icon = NEXT_GRAPHICS_ACTION_ICON

      override val canClick: Boolean
        get() = lastIndex < lastNormal.lastIndex

      override fun onClick() {
        lastIndex++
        showCurrent()
        postSnapshotNumber()
      }
    }

    class ExportGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val title = EXPORT_GRAPHICS_ACTION_TITLE
      override val description = EXPORT_GRAPHICS_ACTION_DESCRIPTION
      override val icon = EXPORT_GRAPHICS_ACTION_ICON

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
      override val title = ZOOM_GRAPHICS_ACTION_TITLE
      override val description = ZOOM_GRAPHICS_ACTION_DESCRIPTION
      override val icon = ZOOM_GRAPHICS_ACTION_ICON

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        val snapshot = lastZoomed[lastIndex]
        Desktop.getDesktop().open(snapshot.file)
      }
    }

    class ClearGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val title = CLEAR_GRAPHICS_ACTION_TITLE
      override val description = CLEAR_GRAPHICS_ACTION_DESCRIPTION
      override val icon = CLEAR_GRAPHICS_ACTION_ICON

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        lastNumber?.let { number ->
          repository.clearSnapshot(number)
        }
      }
    }

    class ClearAllGraphicsActionHolder : RGraphicsToolbar.ActionHolder {
      override val title = CLEAR_ALL_GRAPHICS_ACTION_TITLE
      override val description = CLEAR_ALL_GRAPHICS_ACTION_DESCRIPTION
      override val icon = CLEAR_ALL_GRAPHICS_ACTION_ICON

      override val canClick: Boolean
        get() = lastNormal.isNotEmpty()

      override fun onClick() {
        repository.clearAllSnapshots()
      }
    }

    return listOf(
      RGraphicsToolbar.groupOf(
        PreviousGraphicsActionHolder(),
        NextGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ExportGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ZoomGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ClearGraphicsActionHolder()
      ),
      RGraphicsToolbar.groupOf(
        ClearAllGraphicsActionHolder()
      )
    )
  }

  private fun getAdjustedScreenDimension(): Dimension {
    val panelDimension = graphicsPanel.component.size
    val toolPanelHeight = graphicsPanel.getToolPanelHeight() ?: 0
    return Dimension(panelDimension.width, panelDimension.height - toolPanelHeight)
  }

  private fun postScreenDimension() {
    repository.configuration?.let { oldConfiguration ->
      val parameters = oldConfiguration.screenParameters
      val newParameters = parameters.copy(dimension = getAdjustedScreenDimension())
      settingsSubDialog.currentParameters = newParameters
      repository.configuration = oldConfiguration.copy(screenParameters = newParameters)
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
    private const val RESIZE_SPLIT_WEIGHT = 0.67

    private val MARGINS_TOO_LARGE_MESSAGE = RBundle.message("graphics.panel.error.margins.too.large")

    private val EXPORT_GRAPHICS_ACTION_TITLE = RBundle.message("graphics.panel.action.export.title")
    private val ZOOM_GRAPHICS_ACTION_TITLE = RBundle.message("graphics.panel.action.zoom.title")
    private val CLEAR_GRAPHICS_ACTION_TITLE = RBundle.message("graphics.panel.action.clear.title")
    private val CLEAR_ALL_GRAPHICS_ACTION_TITLE = RBundle.message("graphics.panel.action.clear.all.title")

    private val PREVIOUS_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.previous.description")
    private val NEXT_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.next.description")
    private val EXPORT_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.export.description")
    private val ZOOM_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.zoom.description")
    private val CLEAR_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.clear.description")
    private val CLEAR_ALL_GRAPHICS_ACTION_DESCRIPTION = RBundle.message("graphics.panel.action.clear.all.description")

    private val PREVIOUS_GRAPHICS_ACTION_ICON = AllIcons.Actions.Back
    private val NEXT_GRAPHICS_ACTION_ICON = AllIcons.Actions.Forward
    private val EXPORT_GRAPHICS_ACTION_ICON = AllIcons.Actions.Menu_saveall
    private val ZOOM_GRAPHICS_ACTION_ICON = AllIcons.Actions.Search
    private val CLEAR_GRAPHICS_ACTION_ICON = AllIcons.Actions.Close  // TODO [mine]: adequate icons, please
    private val CLEAR_ALL_GRAPHICS_ACTION_ICON = AllIcons.Actions.Cancel
    private val TUNE_GRAPHICS_DEVICE_ACTION_ICON = AllIcons.General.GearPlain

    private val RESOLUTION_CHANGED_MESSAGE = RBundle.message("graphics.panel.settings.resolution.changed.message")
    private val RESOLUTION_CHANGED_TITLE = RBundle.message("graphics.panel.settings.resolution.changed.title")

    private fun createDestinationFile(file: File) {
      if (!file.exists() && !file.createNewFile()) {
        throw RuntimeException(RBundle.message("graphics.panel.file.saver.failure.details"))
      }
    }
  }
}