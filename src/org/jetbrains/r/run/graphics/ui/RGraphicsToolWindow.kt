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
import org.intellij.datavis.r.inlays.components.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RGraphicsSettings
import org.intellij.datavis.r.ui.ToolbarUtil
import org.jetbrains.r.run.graphics.*
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent

class RGraphicsToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
  private var lastOutputs = listOf<RGraphicsOutput>()
  private var lastIndex = -1
  private var maxNumber = -1

  private val lastOutput: RGraphicsOutput?
    get() = if (lastIndex in lastOutputs.indices) lastOutputs[lastIndex] else null

  private val lastNumber: Int?
    get() = lastOutput?.number

  private var resolution = RGraphicsSettings.getScreenParameters(project).resolution ?: RGraphicsUtils.DEFAULT_RESOLUTION
  private var isStandalone = RGraphicsSettings.isStandalone(project)

  private val queue = MergingUpdateQueue(RESIZE_TASK_NAME, RESIZE_TIME_SPAN, true, null, project)
  private val repository = RGraphicsRepository.getInstance(project)
  private val graphicsPanel = GraphicsPanel(project, project)
  private val plotViewer = RPlotViewer()

  init {
    updateContent()
    toolbar = createToolbar(project)
    project.messageBus.syncPublisher(CHANGE_DARK_MODE_TOPIC).onDarkModeChanged(RGraphicsSettings.isDarkModeEnabled(project))

    graphicsPanel.component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        if (!isStandalone) {
          queue.queue(object : Update(RESIZE_TASK_IDENTITY) {
            override fun run() {
              postScreenParameters()
            }
          })
        }
      }
    })

    repository.addUpdateListener { update ->
      ApplicationManager.getApplication().invokeLater {
        refresh(update)
      }
    }
  }

  private fun updateContent() {
    val targetContent = if (isStandalone) plotViewer else graphicsPanel.component
    if (content !== targetContent) {
      setContent(targetContent)
    }
  }

  private fun refresh(update: RGraphicsUpdate) {
    when (update) {
      is RGraphicsLoadingUpdate -> refreshLoading(update.loadedCount, update.totalCount)
      is RGraphicsCompletedUpdate -> refreshCompleted(update.outputs)
    }
  }

  private fun refreshLoading(loadedCount: Int, totalCount: Int) {
    val message = RBundle.message("graphics.panel.loading.snapshots.hint", loadedCount, totalCount)
    updateContent()
    graphicsPanel.showLoadingMessage(message)
  }

  private fun refreshCompleted(outputs: List<RGraphicsOutput>) {
    if (outputs.isNotEmpty()) {
      val newMaxNumber = outputs.last().number
      // Note: `newMaxNumber` is bigger if there is a new output
      lastIndex = if (maxNumber == newMaxNumber) suggestOutputIndex(outputs) else outputs.lastIndex
      maxNumber = newMaxNumber
      lastOutputs = outputs
      showCurrent()
    } else {
      reset()
    }
    postOutputNumber()
    postScreenParametersIfNeeded()
  }

  private fun suggestOutputIndex(outputs: List<RGraphicsOutput>): Int {
    return suggestOutputIndexOrNull(outputs)?.takeIf { it in outputs.indices } ?: outputs.lastIndex
  }

  private fun suggestOutputIndexOrNull(outputs: List<RGraphicsOutput>): Int? {
    return loadOutputNumber()?.let { number ->
      outputs.indexWith(number) ?: lastOutputs.indexWith(number)
    }
  }

  private fun reset() {
    lastOutputs = listOf()
    lastIndex = -1
    maxNumber = -1
    setContent(graphicsPanel.component)  // Note: fall back to graphics panel which will show "No graphics" message
    graphicsPanel.reset()
  }

  private fun showCurrent() {
    lastOutput?.let { output ->
      updateContent()
      if (isStandalone) {
        plotViewer.resolution = resolution
        plotViewer.plot = output.plot
      } else {
        if (output.snapshot != null) {
          graphicsPanel.showImage(output.snapshot.file)
        } else {
          graphicsPanel.showLoadingMessage(RESCALING_HINT)
        }
      }
    }
  }

  private fun createToolbar(project: Project): JComponent {
    return ToolbarUtil.createToolbar(RToolWindowFactory.PLOTS, createActionHolderGroups(), DarkModeCheckBox(project))
  }

  private fun createActionHolderGroups(): List<List<ToolbarUtil.ActionHolder>> {
    val hasOutputs = { lastOutputs.isNotEmpty() }
    val groups = listOf(
      listOf(
        Triple(PREVIOUS_GRAPHICS_ACTION_ID, { lastIndex > 0 }, this::moveToPreviousOutput),
        Triple(NEXT_GRAPHICS_ACTION_ID, { lastIndex < lastOutputs.lastIndex }, this::moveToNextOutput)
      ),
      listOf(
        Triple(EXPORT_GRAPHICS_ACTION_ID, hasOutputs, this::exportCurrentOutput),
        Triple(COPY_GRAPHICS_ACTION_ID, hasOutputs, this::copyCurrentOutput),
        Triple(ZOOM_GRAPHICS_ACTION_ID, hasOutputs, this::zoomCurrentOutput),
        Triple(CLEAR_GRAPHICS_ACTION_ID, hasOutputs, this::clearCurrentOutput)
      ),
      listOf(
        Triple(CLEAR_ALL_GRAPHICS_ACTION_ID, hasOutputs, this::clearAllOutputs)
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

  private fun moveToPreviousOutput() {
    lastIndex--
    showCurrent()
    postOutputNumber()
  }

  private fun moveToNextOutput() {
    lastIndex++
    showCurrent()
    postOutputNumber()
  }

  private fun exportCurrentOutput() {
    TODO()
  }

  private fun copyCurrentOutput() {
    TODO()
  }

  private fun zoomCurrentOutput() {
    TODO()
  }

  private fun clearCurrentOutput() {
    lastNumber?.let { number ->
      repository.clearOutput(number)
    }
  }

  private fun clearAllOutputs() {
    repository.clearAllOutputs()
  }

  private fun showSettingsDialog() {
    RGraphicsSettingsDialogEx.show(resolution, isStandalone) { newResolution, newStandalone ->
      if (newResolution != resolution) {
        val oldParameters = RGraphicsSettings.getScreenParameters(project)
        val newParameters = oldParameters.copy(resolution = newResolution)
        RGraphicsSettings.setScreenParameters(project, newParameters)
        resolution = newResolution
      }
      if (newStandalone != isStandalone) {
        RGraphicsSettings.setStandalone(project, newStandalone)
        isStandalone = newStandalone
      }
      showCurrent()
      postScreenParametersIfNeeded()
    }
  }

  private fun getAdjustedScreenDimension(): Dimension {
    return if (isStandalone) plotViewer.size else graphicsPanel.imageComponentSize
  }

  private fun postScreenParametersIfNeeded() {
    if (!isStandalone) {
      postScreenParameters()
    }
  }

  private fun postScreenParameters() {
    val newDimension = getAdjustedScreenDimension()
    if (newDimension.isValid) {
      RGraphicsSettings.setScreenDimension(project, newDimension)
      repository.configuration?.let { oldConfiguration ->
        val parameters = oldConfiguration.screenParameters
        val newParameters = parameters.copy(dimension = newDimension, resolution = resolution)
        repository.configuration = oldConfiguration.copy(screenParameters = newParameters)
      }
    }
  }

  private fun postOutputNumber() {
    repository.configuration?.let { oldConfiguration ->
      repository.configuration = oldConfiguration.copy(snapshotNumber = lastNumber)
    }
  }

  private fun loadOutputNumber(): Int? {
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

    private val RESCALING_HINT = RBundle.message("graphics.panel.rescaling.hint")

    private val DARK_MODE_TITLE = RBundle.message("graphics.panel.action.darkMode.title")
    private val DARK_MODE_DESCRIPTION = RBundle.message("graphics.panel.action.darkMode.description")

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0

    private fun List<RGraphicsOutput>.indexWith(number: Int): Int? {
      return indexOfLast { it.number == number }.takeIf { it >= 0 }
    }
  }
}