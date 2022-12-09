// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.jetbrains.plugins.notebooks.visualization.r.inlays.ClipboardUtils
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.CHANGE_DARK_MODE_TOPIC
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.GraphicsPanel
import org.jetbrains.plugins.notebooks.visualization.r.ui.ToolbarUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.actions.RDumbAwareBgtAction
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.run.graphics.*
import org.jetbrains.r.settings.RGraphicsSettings
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
  private val plotViewer = RPlotViewer(project, project)

  private val usesPlotViewer: Boolean
    get() = isStandalone && lastOutput?.plot?.error == null

  init {
    updateContent()
    toolbar = createToolbar(project)
    project.messageBus.syncPublisher(CHANGE_DARK_MODE_TOPIC).onDarkModeChanged(RGraphicsSettings.isDarkModeEnabled(project))
    RGraphicsSettings.addStandaloneListener(project, project) { newStandalone ->
      isStandalone = newStandalone
      showCurrent()
      postScreenParameters()
    }

    graphicsPanel.component.addResizeListener {
      schedulePostScreenParameters()
    }
    plotViewer.addResizeListener {
      schedulePostScreenParameters()
    }

    repository.addUpdateListener { update ->
      ApplicationManager.getApplication().invokeLater {
        refresh(update)
      }
    }
  }

  private fun updateContent() {
    val targetContent = if (usesPlotViewer) plotViewer else graphicsPanel.component
    updateContent(targetContent)
  }

  private fun updateContent(targetContent: JComponent) {
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
    updateContent(graphicsPanel.component)
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
    postScreenParameters()
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
      if (usesPlotViewer) {
        showPlot(output.plot)
      } else {
        if (output.snapshot != null) {
          graphicsPanel.showImage(output.snapshot.file)
        } else {
          graphicsPanel.showLoadingMessage(RESCALING_HINT)
        }
      }
    }
  }

  private fun showPlot(plot: RPlot) {
    plotViewer.resolution = resolution
    plotViewer.plot = plot
  }

  private fun createToolbar(project: Project): JComponent {
    return ToolbarUtil.createToolbar(RToolWindowFactory.PLOTS, createActionHolderGroups(), EngineComboBox(), DarkModeCheckBox(project))
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
    lastOutput?.let { output ->
      val initialSize = getAdjustedScreenDimension()
      if (usesPlotViewer) {
        RGraphicsExportDialog.show(project, project, output.plot, initialSize, resolution)
      } else {
        output.snapshot?.let { snapshot ->
          RGraphicsExportDialog.show(project, project, snapshot, initialSize)
        }
      }
    }
  }

  private fun copyCurrentOutput() {
    lastOutput?.let {
      val image = if (usesPlotViewer) plotViewer.image else graphicsPanel.image
      if (image != null) {
        ClipboardUtils.copyImageToClipboard(image)
      }
    }
  }

  private fun zoomCurrentOutput() {
    lastOutput?.let { output ->
      if (usesPlotViewer) {
        RGraphicsZoomDialog.show(project, project, output.plot, resolution)
      } else {
        output.snapshot?.let { snapshot ->
          RGraphicsZoomDialog.show(project, project, snapshot)
        }
      }
    }
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
    RGraphicsSettingsDialogEx.show(resolution) { newResolution ->
      if (newResolution != resolution) {
        val oldParameters = RGraphicsSettings.getScreenParameters(project)
        val newParameters = oldParameters.copy(resolution = newResolution)
        RGraphicsSettings.setScreenParameters(project, newParameters)
        resolution = newResolution
      }
      showCurrent()
      postScreenParameters()
    }
  }

  private fun getAdjustedScreenDimension(): Dimension {
    return if (usesPlotViewer) plotViewer.size else graphicsPanel.imageComponentSize
  }

  private fun schedulePostScreenParameters() {
    queue.queue(object : Update(RESIZE_TASK_IDENTITY) {
      override fun run() {
        postScreenParameters()
      }
    })
  }

  private fun postScreenParameters() {
    val newDimension = getAdjustedScreenDimension()
    if (newDimension.isValid) {
      RGraphicsSettings.setScreenDimension(project, newDimension)
      val isRescalingEnabled = !usesPlotViewer
      if (isRescalingEnabled) {
        repository.configuration?.let { oldConfiguration ->
          val parameters = oldConfiguration.screenParameters
          val newParameters = parameters.copy(dimension = newDimension, resolution = resolution)
          repository.configuration = oldConfiguration.copy(screenParameters = newParameters, isRescalingEnabled = true)
        }
      }
    }
  }

  private fun postOutputNumber() {
    repository.configuration?.let { oldConfiguration ->
      repository.configuration = oldConfiguration.copy(isRescalingEnabled = !usesPlotViewer, snapshotNumber = lastNumber)
    }
  }

  private fun loadOutputNumber(): Int? {
    return repository.configuration?.snapshotNumber
  }

  private inner class EngineComboBox : ComboBoxAction(), DumbAware {
    override fun update(e: AnActionEvent) {
      val canRenderPlot = lastOutput?.plot?.error == null
      e.presentation.text = if (isStandalone && canRenderPlot) ENGINE_IDE_TEXT else ENGINE_R_TEXT
      e.presentation.description = if (canRenderPlot) ENGINE_TOOLTIP else ENGINE_CORRUPTED_TOOLTIP
      e.presentation.isEnabled = canRenderPlot
    }

    override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
      val ideAction = createAction(ENGINE_IDE_TEXT, ENGINE_IDE_DESCRIPTION, newStandalone = true)
      val rAction = createAction(ENGINE_R_TEXT, ENGINE_R_DESCRIPTION, newStandalone = false)
      return DefaultActionGroup(ideAction, rAction)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun createAction(text: String, description: String, newStandalone: Boolean): AnAction {
      return object : RDumbAwareBgtAction(text, description, null) {
        override fun actionPerformed(e: AnActionEvent) {
          setStandalone(newStandalone)
        }
      }
    }

    private fun setStandalone(newStandalone: Boolean) {
      if (newStandalone != isStandalone) {
        RGraphicsSettings.setStandalone(project, newStandalone)
      }
    }
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

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
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

    private val SWITCH_TO_BUILTIN_TEXT = RBundle.message("plot.viewer.switch.to.builtin")

    private val ENGINE_TOOLTIP = RBundle.message("graphics.panel.engine.tooltip")
    private val ENGINE_CORRUPTED_TOOLTIP = RBundle.message("graphics.panel.engine.corrupted.tooltip")
    private val ENGINE_IDE_TEXT = RBundle.message("graphics.panel.engine.ide.text")
    private val ENGINE_IDE_DESCRIPTION = RBundle.message("graphics.panel.engine.ide.description")
    private val ENGINE_R_TEXT = RBundle.message("graphics.panel.engine.r.text")
    private val ENGINE_R_DESCRIPTION = RBundle.message("graphics.panel.engine.r.description")

    private val DARK_MODE_TITLE = RBundle.message("graphics.panel.action.darkMode.title")
    private val DARK_MODE_DESCRIPTION = RBundle.message("graphics.panel.action.darkMode.description")

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0

    private fun List<RGraphicsOutput>.indexWith(number: Int): Int? {
      return indexOfLast { it.number == number }.takeIf { it >= 0 }
    }

    private fun JComponent.addResizeListener(listener: () -> Unit) {
      addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
          listener()
        }
      })
    }
  }
}