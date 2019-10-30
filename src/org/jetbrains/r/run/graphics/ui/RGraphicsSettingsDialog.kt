/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.run.graphics.RGraphicsUtils
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.text.DecimalFormat
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.math.round

class RGraphicsSettingsDialog(
  private val listener: Listener
) : DialogWrapper(null, true) {

  private val titleLabel = JLabel(TITLE, JLabel.LEFT).apply {
    font = font.deriveFont(font.getStyle() or Font.BOLD)
  }

  private val autoResizeCheckBox = JBCheckBox(AUTO_RESIZE_TEXT, true).apply {
    addItemListener { e ->
      val isSelected = e.stateChange == ItemEvent.SELECTED
      updateControlsEditable(!isSelected)
      listener.onAutoResizeSwitch(isSelected)
    }
  }

  private val widthInputField = createNumberInputField { currentUnitModel }
  private val heightInputField = createNumberInputField { currentUnitModel }
  private val resolutionInputField = createNumberInputField { pixelUnitModel }
  private val widthUnitComboBox = createUnitComboBox()
  private val heightUnitComboBox = createUnitComboBox()

  private val pixelUnitModel = PixelUnitModel()
  private val inchUnitModel = InchUnitModel { resolutionInputField.text.toIntOrNull() }
  private val centimeterUnitModel = CentimeterUnitModel(inchUnitModel)
  private var currentUnitModel: UnitModel = pixelUnitModel

  var currentParameters: RGraphicsUtils.ScreenParameters?
    get() {
      val width = currentUnitModel.convertInputToPixels(widthInputField.text)
      val height = currentUnitModel.convertInputToPixels(heightInputField.text)
      val resolution = resolutionInputField.text.toIntOrNull()
      return if (width != null && height != null && resolution != null) {
        RGraphicsUtils.ScreenParameters(Dimension(width, height), resolution)
      } else {
        null
      }
    }
    set(parameters) {
      resolutionInputField.text = parameters?.resolution?.toString() ?: ""  // Note: resolution must be set first
      widthInputField.text = currentUnitModel.convertPixelsToInput(parameters?.width)
      heightInputField.text = currentUnitModel.convertPixelsToInput(parameters?.height)
    }

  val isAutoResizeEnabled: Boolean
    get() = autoResizeCheckBox.isSelected

  init {
    title = TITLE
    init()
    updateControlsEditable(false)
  }

  override fun createCenterPanel(): JComponent {
    fun JPanel.addToGrid(component: JComponent, xGrid: Int, yGrid: Int, gridWidth: Int = 1, xWeight: Double = 0.0, yWeight: Double? = null) {
      val constraints = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        insets = JBUI.insets(2)
        gridx = xGrid
        gridy = yGrid
        gridwidth = gridWidth
        weightx = xWeight
        if (yWeight != null) {
          weighty = yWeight
        }
      }
      add(component, constraints)
    }

    fun JPanel.addInput(inputField: JTextField, text: String, lastComponent: JComponent, index: Int) {
      addToGrid(JLabel(text, JLabel.LEFT), 0, index)
      addToGrid(inputField, 1, index)
      addToGrid(lastComponent, 2, index)
    }

    return JPanel(GridBagLayout()).apply {
      addToGrid(titleLabel, 0, 0, 3)
      addToGrid(autoResizeCheckBox, 0, 1, 3)
      addInput(widthInputField, PLOT_WIDTH_TEXT, widthUnitComboBox, 2)
      addInput(heightInputField, PLOT_HEIGHT_TEXT, heightUnitComboBox, 3)
      addInput(resolutionInputField, PLOT_RESOLUTION_TEXT, JLabel(DPI_TEXT, JLabel.LEFT), 4)
      titleLabel.preferredSize = widthInputField.preferredSize
      autoResizeCheckBox.preferredSize = widthInputField.preferredSize
    }
  }

  fun createComponent(): JComponent {
    return createCenterPanel()
  }

  private fun createNumberInputField(getUnitModel: () -> UnitModel): JTextField {
    return JTextField(INPUT_FIELD_NUM_COLUMNS).also { field ->
      field.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          val errorText = getUnitModel().validateInput(field.text)
          setErrorText(errorText, field)
        }
      })
      field.addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent?) {
          onInputFieldValueChange()
        }
      })
      field.addActionListener(object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
          onInputFieldValueChange()
        }
      })
    }
  }

  private fun onInputFieldValueChange() {
    currentParameters?.let(listener::onParametersChange)
  }

  private fun createUnitComboBox(): ComboBox<String> {
    return ComboBox<String>().apply {
      addItem(PIXELS_TEXT)
      addItem(INCHES_TEXT)
      addItem(CENTIMETERS_TEXT)
      addItemListener { event ->
        if (event.stateChange == ItemEvent.SELECTED) {
          val selection = selectedItem as String
          updateUnitSelection(selection)
        }
      }
    }
  }

  private fun updateUnitSelection(selection: String) {
    val selectedModel = when (selection) {
      PIXELS_TEXT -> pixelUnitModel
      INCHES_TEXT -> inchUnitModel
      CENTIMETERS_TEXT -> centimeterUnitModel
      else -> throw RuntimeException("Unsupported selection: $selection")
    }
    if (currentUnitModel != selectedModel) {
      val width = currentUnitModel.convertInputToPixels(widthInputField.text)
      val height = currentUnitModel.convertInputToPixels(heightInputField.text)
      currentUnitModel = selectedModel
      widthInputField.text = selectedModel.convertPixelsToInput(width)
      heightInputField.text = selectedModel.convertPixelsToInput(height)
    }
    widthUnitComboBox.selectedItem = selection
    heightUnitComboBox.selectedItem = selection
  }

  private fun updateControlsEditable(areEditable: Boolean) {
    widthInputField.isEditable = areEditable
    heightInputField.isEditable = areEditable
    widthUnitComboBox.isEditable = areEditable
    heightUnitComboBox.isEditable = areEditable
  }

  interface UnitModel {
    val text: String
    fun validateInput(input: String): String?
    fun convertInputToPixels(input: String): Int?
    fun convertPixelsToInput(pixels: Int?): String
  }

  class PixelUnitModel : UnitModel {
    override val text = PIXELS_TEXT

    override fun validateInput(input: String): String? {
      val value = input.toIntOrNull()
      return if (value != null && value > 0) null else INVALID_INTEGER_INPUT_TEXT
    }

    override fun convertInputToPixels(input: String): Int? {
      return input.toIntOrNull()
    }

    override fun convertPixelsToInput(pixels: Int?): String {
      return "${pixels ?: ""}"
    }
  }

  class InchUnitModel(private val getResolution: () -> Int?) : UnitModel {
    private val decimalFormat = DecimalFormat("#.####")

    override val text = INCHES_TEXT

    override fun validateInput(input: String): String? {
      val value = input.toDoubleOrNull()
      return if (value != null && value > 0.0) null else INVALID_DECIMAL_INPUT_TEXT
    }

    override fun convertInputToPixels(input: String): Int? {
      return convertInputToFractionalPixels(input)?.let { round(it).toInt() }
    }

    override fun convertPixelsToInput(pixels: Int?): String {
      return format(convertPixelsToInches(pixels))
    }

    fun convertInputToFractionalPixels(input: String): Double? {
      return input.toDoubleOrNull()?.let { inches ->
        getResolution()?.let { dpi ->
          inches * dpi
        }
      }
    }

    fun convertPixelsToInches(pixels: Int?): Double? {
      return getResolution()?.let { dpi ->
        pixels?.let { pxs ->
          pxs.toDouble() / dpi.toDouble()
        }
      }
    }

    fun format(value: Double?): String {
      return if (value != null) {
        decimalFormat.format(value)
      } else {
        ""
      }
    }
  }

  class CentimeterUnitModel(private val inchUnitModel: InchUnitModel) : UnitModel {
    override val text = CENTIMETERS_TEXT

    override fun validateInput(input: String): String? {
      return inchUnitModel.validateInput(input)
    }

    override fun convertInputToPixels(input: String): Int? {
      return inchUnitModel.convertInputToFractionalPixels(input)?.let { round(it / CMS_PER_INCH).toInt() }
    }

    override fun convertPixelsToInput(pixels: Int?): String {
      val cms = inchUnitModel.convertPixelsToInches(pixels)?.let { it * CMS_PER_INCH }
      return inchUnitModel.format(cms)
    }
  }

  interface Listener {
    fun onParametersChange(parameters: RGraphicsUtils.ScreenParameters)
    fun onAutoResizeSwitch(isEnabled: Boolean)
  }

  companion object {
    private const val CMS_PER_INCH = 2.54
    private const val INPUT_FIELD_NUM_COLUMNS = 8

    private val TITLE = RBundle.message("graphics.panel.settings.dialog.title")
    private val AUTO_RESIZE_TEXT = RBundle.message("graphics.panel.settings.dialog.auto.resize")
    private val PLOT_WIDTH_TEXT = RBundle.message("graphics.panel.settings.dialog.width")
    private val PLOT_HEIGHT_TEXT = RBundle.message("graphics.panel.settings.dialog.height")
    private val PLOT_RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val INVALID_INTEGER_INPUT_TEXT = RBundle.message("graphics.panel.settings.dialog.invalid.integer.input")
    private val INVALID_DECIMAL_INPUT_TEXT = RBundle.message("graphics.panel.settings.dialog.invalid.decimal.input")

    private val PIXELS_TEXT = RBundle.message("graphics.panel.settings.dialog.pixels")
    private val INCHES_TEXT = RBundle.message("graphics.panel.settings.dialog.inches")
    private val CENTIMETERS_TEXT = RBundle.message("graphics.panel.settings.dialog.cm")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")
  }
}
