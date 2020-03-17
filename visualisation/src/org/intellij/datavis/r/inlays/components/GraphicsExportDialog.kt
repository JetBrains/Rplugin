/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.inlays.components.forms.GraphicsExportDialogForm
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.math.round
import kotlin.reflect.KProperty

class GraphicsExportDialog(private val project: Project, parent: Disposable, imagePath: String, initialSize: Dimension?)
  : DialogWrapper(project, null, true, IdeModalityType.MODELESS, false)
{
  private val graphicsManager = GraphicsManager.getInstance(project)
  private val wrapper = GraphicsPanelWrapper(project, parent)
  private val form = GraphicsExportDialogForm()

  private val outputPathInput = TextFieldWithBrowseButton {
    wrapper.image?.let { image ->
      InlayOutputUtil.chooseImageSaveLocation(project, image) { location ->
        outputPath = location.absolutePath
        location.delete()
      }
    }
  }

  private val autoResizeAction =
    object : BasicToggleAction(AUTO_RESIZE_ACTIVE_TEXT, AUTO_RESIZE_IDLE_TEXT, AllIcons.General.FitContent, true) {
      override fun onClick() {
        wrapper.isAutoResizeEnabled = state
        updateSize(null)
      }
    }

  private val keepAspectRatioAction =
    object : BasicToggleAction(KEEP_ASPECT_RATIO_ACTIVE_TEXT, KEEP_ASPECT_RATIO_IDLE_TEXT, AllIcons.Graph.SnapToGrid, false) {
      override fun update(e: AnActionEvent) {
        val isEnabled = !isAutoResizeEnabled && checkSizeInputs()
        e.presentation.isEnabled = isEnabled
        state = state && isEnabled
        super.update(e)  // Note: late call of super method is intentional (as it must see latest value of `state`)
        onClick()
      }

      override fun onClick() {
        updateHeightEnabled()
        updateAspectRatio()
      }
    }

  private val refreshAction = object : DumbAwareAction(REFRESH_PREVIEW_TEXT, REFRESH_PREVIEW_TEXT, AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
      rescaleIfNecessary()
    }

    override fun update(e: AnActionEvent) {
      super.update(e)
      e.presentation.isEnabled = checkSizeInputs() && checkResolutionInput()
    }
  }

  private val isAutoResizeEnabled: Boolean
    get() = autoResizeAction.state

  private val keepAspectRatio: Boolean
    get() = keepAspectRatioAction.state

  private var imageWidth: Int? by IntFieldDelegate(form.widthTextField)
  private var imageHeight: Int? by IntFieldDelegate(form.heightTextField)
  private var imageResolution: Int? by IntFieldDelegate(form.resolutionTextField)

  private var outputPath: String
    get() = outputPathInput.text
    set(value) {
      outputPathInput.text = value
      updateOkAction()
    }

  private var zoomGroup: Disposable? = null
  private var aspectRatio: Double? = null

  init {
    setupGraphicsContentPanel(initialSize)
    createImageGroup(parent, imagePath)
    setOKButtonText(SAVE_BUTTON_TEXT)
    fillSouthPanel()
    title = TITLE
    init()
    imageResolution = wrapper.localResolution
    removeMarginsIfPossible()
    updateSize(initialSize)
  }

  override fun createCenterPanel(): JComponent {
    return form.contentPane.apply {
      form.keepAspectRatioButtonPanel.add(createButton(keepAspectRatioAction))
      form.autoResizeButtonPanel.add(createButton(autoResizeAction))
      form.refreshButtonPanel.add(createButton(refreshAction))
      form.graphicsContentPanel.add(wrapper.component)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    zoomGroup?.dispose()
    wrapper.image?.let { image ->
      val destination = File(outputPath)
      ImageIO.write(image, destination.extension, destination)
    }
  }

  override fun doCancelAction() {
    super.doCancelAction()
    zoomGroup?.dispose()
  }

  private fun setupGraphicsContentPanel(imageSize: Dimension?) {
    val region = imageSize?.let { GraphicsPanel.calculateRegionForImageSize(it) } ?: defaultImageRegion
    form.graphicsContentPanel.apply {
      form.graphicsContentPanel.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
          updateSizeInput(null)
        }
      })
      preferredSize = region
    }
  }

  private fun createImageGroup(parent: Disposable, imagePath: String) {
    graphicsManager?.createImageGroup(imagePath)?.let { pair ->
      wrapper.addImage(pair.first, GraphicsPanelWrapper.RescaleMode.SCHEDULE_RESCALE_IF_POSSIBLE)
      Disposer.register(parent, pair.second)
      zoomGroup = pair.second
    }
  }

  private fun removeMarginsIfPossible() {
    (rootPane.contentPane as JPanel?)?.let { panel ->
      panel.border = JBUI.Borders.empty()
    }
  }

  private fun createButton(action: AnAction): JComponent {
    val actionGroup = DefaultActionGroup(action)
    val toolbar = createToolbar(actionGroup)
    return toolbar.component
  }

  private fun createToolbar(actionGroup: ActionGroup): ActionToolbar {
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true).also { toolbar ->
      toolbar.setReservePlaceAutoPopupIcon(false)
      if (toolbar is ActionToolbarImpl) {
        toolbar.setForceMinimumSize(true)
      }
    }
  }

  private fun fillSouthPanel() {
    form.okCancelButtonsPanel.add(createOkCancelPanel())
    form.outputPathTextFieldPanel.add(outputPathInput)
    outputPathInput.textField.hint = OUTPUT_PATH_HINT
  }

  private fun createOkCancelPanel(): JComponent {
    val buttons = createActions().map { createJButtonForAction(it) }
    return createButtonsPanel(buttons)
  }

  private fun updateOkAction() {
    isOKActionEnabled = outputPath.isNotBlank() && checkSizeInputs() && checkResolutionInput()
  }

  private fun checkSizeInputs(): Boolean {
    return imageWidth != null && imageHeight != null
  }

  private fun checkResolutionInput(): Boolean {
    return imageResolution != null
  }

  private fun rescaleIfNecessary() {
    imageWidth?.let { width ->
      if (!isAutoResizeEnabled) {
        aspectRatio?.let { ratio ->
          imageHeight = round(width / ratio).toInt()
        }
      }
      imageHeight?.let { height ->
        imageResolution?.let { resolution ->
          wrapper.targetResolution = resolution
          val size = Dimension(width, height)
          wrapper.rescaleIfNecessary(size)
        }
      }
    }
  }

  private fun updateAspectRatio() {
    if (keepAspectRatio) {
      if (aspectRatio == null) {
        aspectRatio = imageWidth?.let { width ->
          imageHeight?.let { height ->
            width.toDouble() / height.toDouble()
          }
        }
      }
    } else {
      aspectRatio = null
    }
  }

  private fun updateSize(imageSize: Dimension?) {
    updateSizeInput(imageSize)
    updateSizeEnabled()
    if (isAutoResizeEnabled) {
      rescaleIfNecessary()
    }
  }

  private fun updateSizeInput(imageSize: Dimension?) {
    if (isAutoResizeEnabled) {
      val size = imageSize ?: GraphicsPanel.calculateImageSizeForRegion(form.graphicsContentPanel.size)
      imageHeight = size.height
      imageWidth = size.width
    }
  }

  private fun updateSizeEnabled() {
    form.widthTextField.isEnabled = !isAutoResizeEnabled
    updateHeightEnabled()
  }

  private fun updateHeightEnabled() {
    form.heightTextField.isEnabled = !isAutoResizeEnabled && !keepAspectRatio
  }

  private open class BasicToggleAction(private val activeText: String, private val idleText: String, icon: Icon, isActive: Boolean)
    : DumbAwareToggleAction(if (isActive) activeText else idleText, if (isActive) activeText else idleText, icon)
  {
    var state = isActive
      protected set

    override fun update(e: AnActionEvent) {
      super.update(e)
      val text = if (state) activeText else idleText
      e.presentation.description = text
      e.presentation.text = text
    }

    override fun isSelected(e: AnActionEvent): Boolean {
      return state
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      this.state = state
      onClick()
    }

    protected open fun onClick() {
      // Do nothing
    }
  }

  private inner class IntFieldDelegate(private val field: JTextField) {
    init {
      field.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          val (_, errorText) = tryParseInput()
          setErrorText(errorText, field)
          updateOkAction()
        }
      })
      field.addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent?) {
          rescaleIfNecessary()
        }
      })
      field.addActionListener {
        rescaleIfNecessary()
      }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
      return tryParseInput().first
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
      field.text = value?.takeIf { it > 0 }?.toString() ?: ""
      updateOkAction()
    }

    private fun tryParseInput(): Pair<Int?, String?> {
      val value = field.text.toIntOrNull()?.takeIf { it > 0 }
      return if (value != null) Pair(value, null) else Pair(null, INVALID_INTEGER_INPUT_MESSAGE)
    }
  }

  companion object {
    private val INVALID_INTEGER_INPUT_MESSAGE = VisualizationBundle.message("inlay.output.image.export.dialog.invalid.input")

    private val KEEP_ASPECT_RATIO_ACTIVE_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.keep.aspect.ratio.active")
    private val KEEP_ASPECT_RATIO_IDLE_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.keep.aspect.ratio.idle")
    private val AUTO_RESIZE_ACTIVE_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.auto.resize.active")
    private val AUTO_RESIZE_IDLE_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.auto.resize.idle")
    private val REFRESH_PREVIEW_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.refresh.preview")

    private val OUTPUT_PATH_HINT = VisualizationBundle.message("inlay.output.image.export.dialog.output.path")
    private val SAVE_BUTTON_TEXT = VisualizationBundle.message("inlay.output.image.export.dialog.save")
    private val TITLE = VisualizationBundle.message("inlay.output.image.export.dialog.title")

    private val defaultImageRegion: Dimension
      get() = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.WIDE)

    private var JTextField.hint: String
      get() = (this as JBTextField?)?.emptyText?.text ?: ""
      set(value) {
        if (this is JBTextField) {
          emptyText.text = value
        }
      }
  }
}
