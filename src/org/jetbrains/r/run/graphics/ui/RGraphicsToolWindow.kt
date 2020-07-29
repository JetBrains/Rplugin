// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.components.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.settings.RGraphicsSettings
import org.intellij.datavis.r.ui.ToolbarUtil
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import javax.swing.JComponent

class RGraphicsToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
  private var lastNormal = listOf<RSnapshot>()
  private var lastIndex = -1
  private var maxNumber = -1

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
    toolbar = createToolbar(project)
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

  private fun refresh(normal: List<RSnapshot>) {
    if (normal.isNotEmpty()) {
      val newMaxNumber = normal.last().number
      // Note: `newMaxNumber` is bigger if there is a new plot
      lastIndex = if (maxNumber == newMaxNumber) suggestSnapshotIndex(normal) else normal.lastIndex
      maxNumber = newMaxNumber
      lastNormal = normal
      showCurrent()
    } else {
      reset()
    }
    postSnapshotNumber()
    if (isAutoResizeEnabled) {
      postScreenDimension()
    }
  }

  private fun suggestSnapshotIndex(normal: List<RSnapshot>): Int {
    return suggestSnapshotIndexOrNull(normal)?.takeIf { it in normal.indices } ?: normal.lastIndex
  }

  private fun suggestSnapshotIndexOrNull(normal: List<RSnapshot>): Int? {
    return loadSnapshotNumber()?.let { number ->
      normal.indexWith(number) ?: lastNormal.indexWith(number)
    }
  }

  private fun reset() {
    lastNormal = listOf()
    lastIndex = -1
    maxNumber = -1
    graphicsPanel.reset()
  }

  private fun showCurrent() {
    lastSnapshot?.let { snapshot ->
      graphicsPanel.showImage(snapshot.file)
    }
  }

  private fun createToolbar(project: Project): JComponent {
    return ToolbarUtil.createToolbar(RToolWindowFactory.PLOTS, createActionHolderGroups(), DarkModeCheckBox(project))
  }

  private fun createActionHolderGroups(): List<List<ToolbarUtil.ActionHolder>> {
    val hasSnapshots = { lastNormal.isNotEmpty() }
    val groups = listOf(
      listOf(
        Triple(PREVIOUS_GRAPHICS_ACTION_ID, { lastIndex > 0 }, this::moveToPreviousSnapshot),
        Triple(NEXT_GRAPHICS_ACTION_ID, { lastIndex < lastNormal.lastIndex }, this::moveToNextSnapshot)
      ),
      listOf(
        Triple(EXPORT_GRAPHICS_ACTION_ID, hasSnapshots, this::exportCurrentSnapshot),
        Triple(COPY_GRAPHICS_ACTION_ID, hasSnapshots, this::copyCurrentSnapshot),
        Triple(ZOOM_GRAPHICS_ACTION_ID, hasSnapshots, this::zoomCurrentSnapshot),
        Triple(CLEAR_GRAPHICS_ACTION_ID, hasSnapshots, this::clearCurrentSnapshot)
      ),
      listOf(
        Triple(CLEAR_ALL_GRAPHICS_ACTION_ID, hasSnapshots, this::clearAllSnapshots)
      ),
      listOf(
        Triple(TUNE_GRAPHICS_DEVICE_ACTION_ID, { true }, this::showSettingsDialog)
      )
    )
    return groups.map { group ->
      group.map { (id, canClick, onClick) ->
        ToolbarUtil.createActionHolder(id, canClick, onClick)
      }
    }
  }

  private fun moveToPreviousSnapshot() {
    lastIndex--
    showCurrent()
    postSnapshotNumber()
  }

  private fun moveToNextSnapshot() {
    lastIndex++
    showCurrent()
    postSnapshotNumber()
  }

  private fun exportCurrentSnapshot() {
    lastFile?.absolutePath?.let { imagePath ->
      val imageSize = getAdjustedScreenDimension()
      if (imageSize.isValid) {
        GraphicsExportDialog(project, project, imagePath, imageSize).show()
      }
    }
  }

  private fun copyCurrentSnapshot() {
    graphicsPanel.image?.let { image ->
      ClipboardUtils.copyImageToClipboard(image)
    }
  }

  private fun zoomCurrentSnapshot() {
    lastFile?.absolutePath?.let { imagePath ->
      GraphicsZoomDialog(project, project, imagePath).show()
    }
  }

  private fun clearCurrentSnapshot() {
    lastNumber?.let { number ->
      repository.clearSnapshot(number)
    }
  }

  private fun clearAllSnapshots() {
    repository.clearAllSnapshots()
  }

  private fun showSettingsDialog() {
    val panelDimension = getAdjustedScreenDimension()
    val initialParameters = getInitialScreenParameters()
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

  private fun getInitialScreenParameters(): RGraphicsUtils.ScreenParameters {
    return repository.configuration?.screenParameters ?: RGraphicsSettings.getScreenParameters(project)
  }

  private fun getAdjustedScreenDimension(): Dimension {
    return graphicsPanel.imageComponentSize
  }

  private fun postScreenDimension() {
    val newDimension = getAdjustedScreenDimension()
    if (newDimension.isValid) {
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

  private class DarkModeCheckBox(private val project: Project): CheckboxAction(DARK_MODE_TITLE, DARK_MODE_DESCRIPTION, null) {
    override fun isSelected(e: AnActionEvent): Boolean {
      return RGraphicsSettings.isDarkModeEnabled(project)
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      val isDarkEditor = EditorColorsManager.getInstance().isDarkEditor()
      presentation.isEnabledAndVisible = isDarkEditor
      val jComponent = presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
      // workaround an issue when look'n'feel doesn't change if the component is invisible/disabled
      if (jComponent?.isVisible == false && isDarkEditor) {
        com.intellij.util.IJSwingUtilities.updateComponentTreeUI(jComponent)
      }
      super.update(e)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      RGraphicsSettings.setDarkMode(project, state)
    }
  }

  companion object {
    const val TOOL_WINDOW_ID = "R Graphics"

    private const val RESIZE_TASK_NAME = "Resize graphics"
    private const val RESIZE_TASK_IDENTITY = "Resizing graphics"
    private const val RESIZE_TIME_SPAN = 500

    private const val PREVIOUS_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RPreviousGraphicsAction"
    private const val NEXT_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RNextGraphicsAction"
    private const val EXPORT_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RExportGraphicsAction"
    private const val COPY_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RCopyGraphicsAction"
    private const val ZOOM_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RZoomGraphicsAction"
    private const val CLEAR_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RClearGraphicsAction"
    private const val CLEAR_ALL_GRAPHICS_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RClearAllGraphicsAction"
    private const val TUNE_GRAPHICS_DEVICE_ACTION_ID = "org.jetbrains.r.run.graphics.ui.RTuneGraphicsDeviceAction"

    private val EXPORT_GRAPHICS_DESCRIPTOR_TITLE = RBundle.message("graphics.panel.file.saver.title")
    private val EXPORT_GRAPHICS_DESCRIPTOR_DESCRIPTION = RBundle.message("graphics.panel.file.saver.description")
    private val EXPORT_GRAPHICS_FAILURE_HEADER = RBundle.message("graphics.panel.file.saver.failure.header")

    private val DARK_MODE_TITLE = RBundle.message("graphics.panel.action.darkMode.title")
    private val DARK_MODE_DESCRIPTION = RBundle.message("graphics.panel.action.darkMode.description")

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0

    private fun createDestinationFile(file: File) {
      if (!file.exists() && !file.createNewFile()) {
        throw RuntimeException(RBundle.message("graphics.panel.file.saver.failure.details"))
      }
    }

    private fun List<RSnapshot>.indexWith(number: Int): Int? {
      return indexOfLast { it.number == number }.takeIf { it >= 0 }
    }
  }
}