/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.run.graphics.RGraphicsUtils
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import java.text.DecimalFormat
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import kotlin.math.round

class RGraphicsSettingsDialog(
  private val panelDimension: Dimension,
  private val initialParameters: RGraphicsUtils.ScreenParameters,
  private val initialAutoResizeEnabled: Boolean,
  private val onSettingsChange: (RGraphicsUtils.ScreenParameters, Boolean) -> Unit
) : DialogWrapper(null, true) {

  // Note: selection is inverted here in order to ensure listener is always fired on dialog startup
  private val autoResizeCheckBox = JBCheckBox(AUTO_RESIZE_TEXT, !initialAutoResizeEnabled).apply {
    addItemListener { e ->
      val isSelected = e.stateChange == ItemEvent.SELECTED
      if (isSelected) {
        currentDimension = panelDimension
      }
      updateControlsEditable(!isSelected)
      updateOkAction()
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

  private var currentWidth: Int?
    get() = currentUnitModel.convertInputToPixels(widthInputField.text)
    set(width) {
      widthInputField.text = currentUnitModel.convertPixelsToInput(width)
    }

  private var currentHeight: Int?
    get() = currentUnitModel.convertInputToPixels(heightInputField.text)
    set(height) {
      heightInputField.text = currentUnitModel.convertPixelsToInput(height)
    }

  private var currentResolution: Int?
    get() = resolutionInputField.text.toIntOrNull()
    set(resolution) {
      resolutionInputField.text = resolution?.toString() ?: ""
    }

  private var currentDimension: Dimension?
    get() = currentWidth?.let { width ->
      currentHeight?.let { height ->
        Dimension(width, height)
      }
    }
    set(dimension) {
      currentWidth = dimension?.width
      currentHeight = dimension?.height
    }

  private var currentParameters: RGraphicsUtils.ScreenParameters?
    get() = currentDimension?.let { dimension ->
      currentResolution?.let { resolution ->
        RGraphicsUtils.ScreenParameters(dimension, resolution)
      }
    }
    set(parameters) {
      currentResolution = parameters?.resolution  // Note: resolution should be assigned at first
      currentDimension = parameters?.dimension
    }

  private var isAutoResizeEnabled: Boolean
    get() = autoResizeCheckBox.isSelected
    set(value) {
      autoResizeCheckBox.isSelected = value
    }

  init {
    title = TITLE
    init()
    currentParameters = initialParameters
    isAutoResizeEnabled = initialAutoResizeEnabled
    // Note: there is no need to call `updateOkAction()` here
    // since it's already been called by switching auto resize flag above
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
      addToGrid(autoResizeCheckBox, 0, 0, 3)
      addInput(widthInputField, PLOT_WIDTH_TEXT, widthUnitComboBox, 1)
      addInput(heightInputField, PLOT_HEIGHT_TEXT, heightUnitComboBox, 2)
      addInput(resolutionInputField, PLOT_RESOLUTION_TEXT, JLabel(DPI_TEXT, JLabel.LEFT), 3)
      autoResizeCheckBox.preferredSize = widthInputField.preferredSize
    }
  }

  override fun doOKAction() {
    super.doOKAction()

    // Note: `currentParameters` are always not null here (see `updateOkAction()` method below)
    currentParameters?.let { parameters ->
      onSettingsChange(parameters, isAutoResizeEnabled)
    }
  }

  private fun createNumberInputField(getUnitModel: () -> UnitModel): JTextField {
    return JTextField().also { field ->
      field.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          val errorText = getUnitModel().validateInput(field.text)
          setErrorText(errorText, field)
          updateOkAction()
        }
      })
    }
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
  }

  private fun updateOkAction() {
    val parameters = currentParameters
    isOKActionEnabled = if (parameters != null) {
      if (isAutoResizeEnabled) {
        isAutoResizeEnabled != initialAutoResizeEnabled || currentResolution != initialParameters.resolution
      } else {
        isAutoResizeEnabled != initialAutoResizeEnabled || parameters != initialParameters
      }
    } else {
      false
    }
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

  companion object {
    private const val CMS_PER_INCH = 2.54

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
