/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import org.intellij.datavis.r.inlays.components.forms.GraphicsSettingsDialogForm
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty

class GraphicsSettingsDialog(
  private val initialSettings: Settings,
  private val onSettingsChange: (Settings) -> Unit
) : DialogWrapper(null, true) {

  private val form = GraphicsSettingsDialogForm()

  private val isAutoResizeEnabled: Boolean
    get() = form.autoResizeCheckBox.isSelected

  private val isDarkModeEnabled: Boolean?
    get() = with(form.darkModeCheckBox) {
      isSelected.takeIf { isVisible }
    }

  private val settings: Settings
    get() = Settings(isAutoResizeEnabled, isDarkModeEnabled, globalResolution, localResolution)

  private var localResolution by ResolutionField(form.localResolutionTextField)
  private var globalResolution by ResolutionField(form.globalResolutionTextField)

  init {
    title = TITLE
    init()
    localResolution = initialSettings.localResolution
    globalResolution = initialSettings.globalResolution
    form.localResolutionTextField.setupResolutionField(initialSettings.localResolution)
    form.globalResolutionTextField.setupResolutionField(initialSettings.globalResolution)
    form.autoResizeCheckBox.setupCheckBox(initialSettings.isAutoResizedEnabled)
    form.darkModeCheckBox.setupCheckBox(initialSettings.isDarkModeEnabled)
    updateOkAction()
  }

  override fun createCenterPanel(): JComponent? {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    onSettingsChange(settings)
  }

  private fun updateOkAction() {
    isOKActionEnabled = checkGlobalResolution() && checkLocalResolution() && settings != initialSettings
  }

  private fun checkGlobalResolution(): Boolean {
    return initialSettings.globalResolution == null || globalResolution != null
  }

  private fun checkLocalResolution(): Boolean {
    return initialSettings.localResolution == null || localResolution != null
  }

  private fun JCheckBox.setupCheckBox(isInitialSelected: Boolean?) {
    if (isInitialSelected != null) {
      isSelected = isInitialSelected
      addItemListener {
        updateOkAction()
      }
    } else {
      isVisible = false
    }
  }

  private fun JTextField.setupResolutionField(initialResolution: Int?) {
    isEnabled = initialResolution != null
    addInputValidator { input ->
      INVALID_INTEGER_INPUT_TEXT.takeIf { input.toResolutionOrNull() == null }
    }
  }

  private fun JTextField.addInputValidator(validator: (String) -> String?) {
    document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        val errorText = validator(text)
        setErrorText(errorText, this@addInputValidator)
        updateOkAction()
      }
    })
  }

  private class ResolutionField(private val field: JTextField) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
      return field.text.toResolutionOrNull()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
      field.text = value?.toString() ?: ""
    }
  }

  data class Settings(
    val isAutoResizedEnabled: Boolean,
    val isDarkModeEnabled: Boolean?,
    val globalResolution: Int?,
    val localResolution: Int?
  )

  companion object {
    private const val TITLE = "Graphics settings"
    private const val CHECKBOX_TEXT = "Auto resize"
    private const val INVALID_INTEGER_INPUT_TEXT = "Expected positive integer"

    private fun String.toResolutionOrNull(): Int? {
      return toIntOrNull()?.takeIf { it > 0 }
    }
  }
}
