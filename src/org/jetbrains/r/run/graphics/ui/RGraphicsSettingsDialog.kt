/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.JBUI
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.ui.RDimensionPreference
import org.jetbrains.r.ui.calculateDialogPreferredSize
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.math.round

class RGraphicsSettingsDialog(
  private val initialParameters: RGraphicsUtils.ScreenParameters,
  private val onParametersChange: (RGraphicsUtils.ScreenParameters) -> Unit
) : DialogWrapper(null, true) {

  private val widthInputField = createNumberInputField { currentUnitModel }
  private val heightInputField = createNumberInputField { currentUnitModel }
  private val resolutionInputField = createNumberInputField { pixelUnitModel }
  private val widthUnitComboBox = createUnitComboBox()
  private val heightUnitComboBox = createUnitComboBox()
  private val hintLabel = JLabel(HINT_TEXT, HINT_ICON, JLabel.LEFT)

  private val pixelUnitModel = PixelUnitModel()
  private val inchUnitModel = InchUnitModel { resolutionInputField.text.toIntOrNull() }
  private val centimeterUnitModel = CentimeterUnitModel(inchUnitModel)
  private var currentUnitModel: UnitModel = pixelUnitModel

  private var currentParameters: RGraphicsUtils.ScreenParameters?
    get() {
      val width = currentUnitModel.convertInputToPixels(widthInputField.text)
      val height = currentUnitModel.convertInputToPixels(heightInputField.text)
      val resolution = resolutionInputField.text.toIntOrNull()
      return if (width != null && height != null && resolution != null) {
        RGraphicsUtils.ScreenParameters(width, height, resolution)
      } else {
        null
      }
    }
    set(parameters) {
      resolutionInputField.text = parameters?.resolution?.toString() ?: ""  // Note: resolution must be set first
      widthInputField.text = currentUnitModel.convertPixelsToInput(parameters?.width)
      heightInputField.text = currentUnitModel.convertPixelsToInput(parameters?.height)
    }

  init {
    title = TITLE
    init()
    currentParameters = initialParameters
    updateOkAction()
  }

  override fun createCenterPanel(): JComponent? {
    fun JPanel.addToGrid(component: JComponent, xGrid: Int, yGrid: Int, gridWidth: Int = 1, xWeight: Double = 0.0) {
      val constraints = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        insets = JBUI.insets(2)
        gridx = xGrid
        gridy = yGrid
        gridwidth = gridWidth
        weightx = xWeight
      }
      add(component, constraints)
    }

    fun JPanel.addInput(inputField: JTextField, text: String, lastComponent: JComponent, index: Int) {
      addToGrid(JLabel(text, JLabel.LEFT), 0, index)
      addToGrid(inputField, 1, index, xWeight = 1.0)
      addToGrid(lastComponent, 2, index)
    }

    fun JPanel.addFullLineNonCollapsing(component: JComponent, index: Int) {
      val containerPanel = JPanel(GridLayout(1, 1)).apply {
        add(component)
      }
      addToGrid(containerPanel, 0, index, 3)
    }

    return JPanel(GridBagLayout()).apply {
      addInput(widthInputField, PLOT_WIDTH_TEXT, widthUnitComboBox, 0)
      addInput(heightInputField, PLOT_HEIGHT_TEXT, heightUnitComboBox, 1)
      addInput(resolutionInputField, PLOT_RESOLUTION_TEXT, JLabel(DPI_TEXT, JLabel.LEFT), 2)
      addFullLineNonCollapsing(hintLabel, 3)
      preferredSize = calculateDialogPreferredSize(RDimensionPreference.VERY_NARROW)
    }
  }

  override fun createLeftSideActions(): Array<Action> {
    val resetAction = object : DialogWrapperAction(DEFAULT_SETTINGS_TEXT) {
      override fun doAction(e: ActionEvent?) {
        currentParameters = RGraphicsUtils.getDefaultScreenParameters()
        updateOkAction()
      }
    }

    return arrayOf(resetAction)
  }

  override fun doOKAction() {
    super.doOKAction()
    currentParameters?.let { parameters ->
      onParametersChange(parameters)
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

  private fun updateOkAction() {
    fun areAllValid(): Boolean {
      fun validateInput(input: String): Boolean {
        return currentUnitModel.validateInput(input) == null
      }

      return validateInput(widthInputField.text) &&
             validateInput(heightInputField.text) &&
             validateInput(resolutionInputField.text)
    }

    val canSave = areAllValid() && currentParameters != initialParameters
    isOKActionEnabled = canSave
    hintLabel.isVisible = canSave
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
    override val text = INCHES_TEXT

    override fun validateInput(input: String): String? {
      val value = input.toDoubleOrNull()
      return if (value != null && value > 0.0) null else INVALID_DECIMAL_INPUT_TEXT
    }

    override fun convertInputToPixels(input: String): Int? {
      return convertInputToFractionalPixels(input)?.let { round(it).toInt() }
    }

    override fun convertPixelsToInput(pixels: Int?): String {
      return "${convertPixelsToInches(pixels) ?: ""}"
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
      return "${inchUnitModel.convertPixelsToInches(pixels)?.let { it * CMS_PER_INCH } ?: ""}"
    }
  }

  companion object {
    private const val CMS_PER_INCH = 2.54

    private val TITLE = RBundle.message("graphics.panel.settings.dialog.title")
    private val PLOT_WIDTH_TEXT = RBundle.message("graphics.panel.settings.dialog.width")
    private val PLOT_HEIGHT_TEXT = RBundle.message("graphics.panel.settings.dialog.height")
    private val PLOT_RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val DEFAULT_SETTINGS_TEXT = RBundle.message("graphics.panel.settings.dialog.default")
    private val INVALID_INTEGER_INPUT_TEXT = RBundle.message("graphics.panel.settings.dialog.invalid.integer.input")
    private val INVALID_DECIMAL_INPUT_TEXT = RBundle.message("graphics.panel.settings.dialog.invalid.decimal.input")
    private val HINT_TEXT = RBundle.message("graphics.panel.settings.dialog.hint")
    private val HINT_ICON = AllIcons.General.WarningDialog

    private val PIXELS_TEXT = RBundle.message("graphics.panel.settings.dialog.pixels")
    private val INCHES_TEXT = RBundle.message("graphics.panel.settings.dialog.inches")
    private val CENTIMETERS_TEXT = RBundle.message("graphics.panel.settings.dialog.cm")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")
  }
}
