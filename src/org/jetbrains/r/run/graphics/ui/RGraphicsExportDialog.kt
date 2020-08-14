/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.JBUI
import org.intellij.datavis.r.VisualizationIcons.CONSTRAIN_IMAGE_PROPORTIONS
import org.intellij.datavis.r.inlays.components.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import org.jetbrains.r.run.graphics.ui.forms.RGraphicsExportDialogForm
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter
import kotlin.math.max
import kotlin.math.round
import kotlin.reflect.KProperty

class RGraphicsExportDialog(private val project: Project, parent: Disposable, imagePath: String, initialSize: Dimension?) :
  BorderlessDialogWrapper(project, TITLE, IdeModalityType.MODELESS)
{
  private val graphicsManager = ChunkGraphicsManager(project)
  private val wrapper = RGraphicsPanelWrapper(project, parent)
  private val form = RGraphicsExportDialogForm()

  private val resizablePanel = RResizablePanel(wrapper.component, initialSize, this::onImageResize).apply {
    wrapper.overlayComponent = manipulator
  }

  private val directoryTextField = TextFieldWithBrowseButton {
    InlayOutputUtil.chooseDirectory(project, CHOOSE_DIRECTORY_TITLE, CHOOSE_DIRECTORY_DESCRIPTION)?.let { directory ->
      outputDirectory = directory.path
    }
  }

  private val keepAspectRatioAction = BasicToggleAction(KEEP_ASPECT_RATIO_PRESENTATION, this::checkSizeInputs) {
    updateAspectRatio()
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
    get() = form.autoResizeCheckBox.isSelected

  private val keepAspectRatio: Boolean
    get() = keepAspectRatioAction.state

  private val imageDimension: Dimension?
    get() = imageWidth?.let { width ->
      imageHeight?.let { height ->
        Dimension(width, height)
      }
    }

  private val widthTextField = createUnitTextField(form.widthInputPanel, PX_TEXT, PX_MAX_CHARACTERS)
  private val heightTextField = createUnitTextField(form.heightInputPanel, PX_TEXT, PX_MAX_CHARACTERS)
  private val resolutionTextField = createUnitTextField(form.resolutionInputPanel, DPI_TEXT, DPI_MAX_CHARACTERS)

  private var imageWidth: Int? by IntFieldDelegate(widthTextField, InputKind.WIDTH)
  private var imageHeight: Int? by IntFieldDelegate(heightTextField, InputKind.HEIGHT)
  private var imageResolution: Int? by IntFieldDelegate(resolutionTextField, null)

  private var zoomGroup: Disposable? = null

  private var aspectRatio: Double? = null
    set(ratio) {
      field = ratio
      resizablePanel.aspectRatio = ratio
    }

  private var outputDirectory: String? = null
    set(directory) {
      field = directory
      directoryTextField.text = directory?.beautifyPath() ?: ""
      updateOkAction()
    }

  private var fileName: String?
    get() = form.fileNameTextField.text.takeIf { it.isNotBlank() }
    set(name) {
      form.fileNameTextField.text = name ?: ""
      updateOkAction()
    }

  init {
    setupGraphicsContentPanel(initialSize)
    createImageGroup(parent, imagePath)
    setOKButtonText(SAVE_BUTTON_TEXT)
    setupAutoResizeCheckBox()
    setupInputControls()
    fillSouthPanel()
    init()
    imageResolution = wrapper.localResolution
    updateSize(initialSize)
  }

  override fun createCenterPanel(): JComponent {
    return form.contentPane.apply {
      form.keepAspectRatioButtonPanel.add(createButton(keepAspectRatioAction))
      form.refreshButtonPanel.add(createButton(refreshAction))
      form.graphicsContentPanel.add(resizablePanel)
    }
  }

  override fun doOKAction() {
    trySaveImage()?.let { location ->
      super.doOKAction()
      zoomGroup?.dispose()
      if (form.openAfterSavingCheckBox.isSelected) {
        Desktop.getDesktop().open(location)
      }
    }
  }

  override fun doCancelAction() {
    super.doCancelAction()
    zoomGroup?.dispose()
  }

  private fun trySaveImage(): File? {
    return wrapper.image?.let { image ->
      outputDirectory?.let { directory ->
        fileName?.let { name ->
          val format = form.formatComboBox.selectedItem as String
          val location = Paths.get(directory, "$name.$format").toFile()
          location.takeIf { checkLocation(it) }?.also {
            ImageIO.write(image, format, location)
            graphicsManager.apply {
              extractImageNumber(name)?.let { number ->
                imageNumber = number
              }
              outputDirectory = directory
            }
          }
        }
      }
    }
  }

  private fun checkLocation(location: File): Boolean {
    if (!location.exists()) {
      return true
    }
    val description = createConfirmReplaceDescription(location)
    return MessageDialogBuilder.yesNo(CONFIRM_REPLACE_TITLE, description)
      .icon(AllIcons.General.WarningDialog).ask(project)
  }

  private fun onImageResize(targetWidth: Int, targetHeight: Int, dx: Int, dy: Int) {
    val previous = size
    val width = previous.width
    val height = previous.height
    val xPadding = width - form.graphicsContentPanel.width
    val yPadding = height - form.graphicsContentPanel.height
    val actualDx = if (dx > 0) max(targetWidth + xPadding - width, 0) else dx
    val actualDy = if (dy > 0) max(targetHeight + yPadding - height, 0) else dy
    setSize(width + actualDx, height + actualDy)
    if (size == previous) {
      form.graphicsContentPanel.revalidate()
    }
  }

  private fun synchronizeSizeInputs(lastKind: InputKind) {
    aspectRatio?.let { ratio ->
      when (lastKind) {
        InputKind.WIDTH -> runWithDisabled(heightTextField) {
          imageHeight = imageWidth?.let { round(it / ratio).toInt() }
        }
        InputKind.HEIGHT -> runWithDisabled(widthTextField) {
          imageWidth = imageHeight?.let { round(it * ratio).toInt() }
        }
      }
    }
  }

  private fun runWithDisabled(field: JTextField, task: () -> Unit) {
    field.isEnabled = false
    task()
    field.isEnabled = true
  }

  private fun setupAutoResizeCheckBox() {
    form.autoResizeCheckBox.addItemListener {
      val state = form.autoResizeCheckBox.isSelected
      wrapper.isAutoResizeEnabled = state
      resizablePanel.isEnabled = state
      updateSize(null)
    }
  }

  private fun setupInputControls() {
    form.fileNameTextField.addBlankTextValidator()
    form.directoryFieldPanel.add(directoryTextField)
    directoryTextField.textField.isFocusable = false
    fileName = graphicsManager.suggestImageName()
    outputDirectory = graphicsManager.outputDirectory ?: project.basePath!!
    form.formatComboBox.apply {
      for (format in InlayOutputUtil.getAvailableFormats()) {
        addItem(format)
      }
      prototypeDisplayValue = "XXXX"  // Note: setup preferred width
    }
  }

  private fun setupGraphicsContentPanel(imageSize: Dimension?) {
    val region = imageSize?.let { GraphicsPanel.calculateRegionForImageSize(it) } ?: defaultImageRegion
    form.graphicsContentPanel.preferredSize = region
    wrapper.component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        updateSizeInput(null)
      }
    })
  }

  private fun createImageGroup(parent: Disposable, imagePath: String) {
    graphicsManager.createImageGroup(imagePath)?.let { pair ->
      wrapper.addImage(pair.first, RGraphicsPanelWrapper.RescaleMode.SCHEDULE_RESCALE_IF_POSSIBLE)
      Disposer.register(parent, pair.second)
      zoomGroup = pair.second
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
  }

  private fun updateOkAction() {
    isOKActionEnabled = checkSizeInputs() && checkResolutionInput() && !outputDirectory.isNullOrBlank() && !fileName.isNullOrBlank()
  }

  private fun checkSizeInputs(): Boolean {
    return imageWidth != null && imageHeight != null
  }

  private fun checkResolutionInput(): Boolean {
    return imageResolution != null
  }

  private fun rescaleIfNecessary() {
    if (Disposer.isDisposed(disposable)) {
      return
    }
    imageDimension?.let { size ->
      imageResolution?.let { resolution ->
        wrapper.targetResolution = resolution
        wrapper.rescaleIfNecessary(size)
      }
    }
  }

  private fun updateAspectRatio() {
    if (keepAspectRatio) {
      if (aspectRatio == null) {
        aspectRatio = imageDimension?.let { size ->
          size.width.toDouble() / size.height.toDouble()
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
      val size = imageSize ?: GraphicsPanel.calculateImageSizeForRegion(wrapper.component.size)
      imageHeight = size.height
      imageWidth = size.width
    }
  }

  private fun updateSizeEnabled() {
    widthTextField.isEnabled = !isAutoResizeEnabled
    heightTextField.isEnabled = !isAutoResizeEnabled
  }

  private fun createUnitTextField(parent: JComponent, unitText: String, maxCharacters: Int): JTextField {
    return UnitTextField(unitText, maxCharacters).apply {
      parent.add(this)
    }
  }

  private fun JTextField.addBlankTextValidator() {
    addTextChangedListener {
      val errorText = BLANK_TEXT_INPUT_MESSAGE.takeIf { text.isBlank() }
      setErrorText(errorText, this)
      updateOkAction()
    }
  }

  private fun String.beautifyPath(): String {
    val path = Paths.get(this).beautify()
    return path.takeIf { it.toString().isNotEmpty() }?.toString() ?: PROJECT_DIRECTORY_HINT
  }

  private fun Path.beautify(): Path {
    val basePath = Paths.get(project.basePath!!)
    return if (startsWith(basePath)) basePath.relativize(this) else this
  }

  private class BasicToggleAction(
    private val presentation: ToggleActionPresentation,
    private val canClick: (() -> Boolean)? = null,
    onClick: (Boolean) -> Unit
  ) : DumbAwareToggleAction()
  {
    private val machine = BiStateMachine(presentation.initialState, onClick)

    val state: Boolean
      get() = machine.state

    init {
      templatePresentation.icon = presentation.icon
      updateText(templatePresentation)
    }

    override fun update(e: AnActionEvent) {
      val isEnabled = canClick?.invoke() ?: true
      e.presentation.isEnabled = isEnabled
      machine.disableIf(!isEnabled)
      updateText(e.presentation)
      super.update(e)  // Note: late call of super method is intentional (as it must see latest value of `machine.state`)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
      return machine.state
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      machine.state = state
    }

    private fun updateText(targetPresentation: Presentation) {
      val text = if (machine.state) presentation.activeText else presentation.idleText
      targetPresentation.description = text
      targetPresentation.text = text
    }
  }

  private data class ToggleActionPresentation(val activeText: String, val idleText: String, val icon: Icon, val initialState: Boolean)

  private class BiStateMachine(initialState: Boolean, private val onChange: (Boolean) -> Unit) {
    var state: Boolean = initialState
      set(newState) {
        if (field != newState) {
          field = newState
          onChange(field)
        }
      }

    fun disableIf(condition: Boolean) {
      if (condition) {
        state = false
      }
    }
  }

  private enum class InputKind {
    WIDTH,
    HEIGHT,
  }

  private inner class IntFieldDelegate(private val field: JTextField, private val kind: InputKind?) {
    init {
      field.addTextChangedListener {
        if (field.isEnabled) {
          val (_, errorText) = tryParseInput()
          setErrorText(errorText, field)
          if (kind != null) {
            synchronizeSizeInputs(kind)
          }
          updateOkAction()
        }
      }
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

  private class UnitTextField(private val unitText: String, maxCharacters: Int) : JTextField() {
    private val fixedSize = calculateSize(unitText, maxCharacters)

    init {
      (document as? AbstractDocument)?.apply {
        documentFilter = LimitedDocumentFilter(maxCharacters)
      }
    }

    private fun calculateSize(unitText: String, maxCharacters: Int): Dimension {
      text = "9".repeat(maxCharacters) + " " + unitText
      return super.getPreferredSize().also {
        text = ""
      }
    }

    override fun getPreferredSize(): Dimension {
      return fixedSize
    }

    override fun getMinimumSize(): Dimension {
      return fixedSize
    }

    override fun paint(g: Graphics) {
      super.paint(g)
      if (g is Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      }
      val fontMetrics = g.fontMetrics
      val unitWidth = fontMetrics.stringWidth("${unitText}X")
      g.color = JBUI.CurrentTheme.Label.disabledForeground()
      g.drawString(unitText, width - unitWidth, height / 2 + fontMetrics.ascent / 2 - 2)
    }
  }

  private class LimitedDocumentFilter(private val maxCharacters: Int) : DocumentFilter() {
    private val maxText = "9".repeat(maxCharacters)

    override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
      if (fb.document.length + text.length - length <= maxCharacters) {
        super.replace(fb, offset, length, text, attrs)
      } else {
        super.replace(fb, 0, fb.document.length, maxText, attrs)
      }
    }
  }

  companion object {
    private const val DPI_MAX_CHARACTERS = 3
    private const val PX_MAX_CHARACTERS = 4

    private val INVALID_INTEGER_INPUT_MESSAGE = RBundle.message("graphics.panel.settings.dialog.invalid.integer.input")
    private val BLANK_TEXT_INPUT_MESSAGE = RBundle.message("graphics.panel.export.dialog.blank.input")

    private val KEEP_ASPECT_RATIO_ACTIVE_TEXT = RBundle.message("graphics.panel.export.dialog.keep.aspect.ratio.active")
    private val KEEP_ASPECT_RATIO_IDLE_TEXT = RBundle.message("graphics.panel.export.dialog.keep.aspect.ratio.idle")
    private val REFRESH_PREVIEW_TEXT = RBundle.message("graphics.panel.export.dialog.refresh.preview")

    private val CHOOSE_DIRECTORY_DESCRIPTION = RBundle.message("graphics.panel.export.dialog.choose.directory.description")
    private val CHOOSE_DIRECTORY_TITLE = RBundle.message("graphics.panel.export.dialog.choose.directory.title")
    private val PROJECT_DIRECTORY_HINT = RBundle.message("import.data.dialog.project.directory.hint")

    private val CONFIRM_REPLACE_TITLE = RBundle.message("graphics.panel.export.dialog.confirm.replace.title")
    private val SAVE_BUTTON_TEXT = RBundle.message("graphics.panel.export.dialog.save")
    private val TITLE = RBundle.message("graphics.panel.export.dialog.title")

    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")
    private val PX_TEXT = RBundle.message("graphics.panel.settings.dialog.pixels")

    private val KEEP_ASPECT_RATIO_PRESENTATION =
      ToggleActionPresentation(KEEP_ASPECT_RATIO_ACTIVE_TEXT, KEEP_ASPECT_RATIO_IDLE_TEXT, CONSTRAIN_IMAGE_PROPORTIONS, false)

    private val defaultImageRegion: Dimension
      get() = DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.WIDE)

    private fun JTextField.addTextChangedListener(listener: (DocumentEvent) -> Unit) {
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          listener(e)
        }
      })
    }

    private fun extractImageNumber(imageName: String): Int? {
      return findLastNumberIndex(imageName)?.let { index ->
        imageName.substring(index).toIntOrNull()
      }
    }

    private fun findLastNumberIndex(s: String): Int? {
      if (s.isEmpty()) {
        return null
      }
      for (i in s.length - 1 downTo 0) {
        if (!s[i].isDigit()) {
          return (i + 1).takeIf { it < s.length }
        }
      }
      return 0
    }

    private fun createConfirmReplaceDescription(location: File): String {
      return RBundle.message("graphics.panel.export.dialog.confirm.replace.description", location.name)
    }
  }
}
