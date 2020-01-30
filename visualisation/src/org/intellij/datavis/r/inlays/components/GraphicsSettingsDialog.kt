/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import org.intellij.datavis.r.inlays.components.forms.GraphicsSettingsDialogForm
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class GraphicsSettingsDialog(
  private val initialAutoResizeEnabled: Boolean,
  private val initialLocalResolution: Int?,
  private val onSettingsChange: (Boolean, Int?) -> Unit
) : DialogWrapper(null, true) {

  private val form = GraphicsSettingsDialogForm()

  private val isAutoResizeEnabled: Boolean
    get() = form.autoResizeCheckBox.isSelected

  private var localResolution: Int?
    get() = form.localResolutionTextField.text.toResolutionOrNull()
    set(resolution) {
      form.localResolutionTextField.text = resolution?.toString() ?: ""
    }

  init {
    title = TITLE
    init()
    form.autoResizeCheckBox.apply {
      isSelected = initialAutoResizeEnabled
      addItemListener {
        updateOkAction()
      }
    }
    localResolution = initialLocalResolution
    form.localResolutionTextField.apply {
      isEnabled = initialLocalResolution != null
      addInputValidator { input ->
        INVALID_INTEGER_INPUT_TEXT.takeIf { input.toResolutionOrNull() == null }
      }
    }
    updateOkAction()
  }

  override fun createCenterPanel(): JComponent? {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    onSettingsChange(isAutoResizeEnabled, localResolution)
  }

  private fun updateOkAction() {
    isOKActionEnabled = checkLocalResolution() && checkSettingsChanged()
  }

  private fun checkLocalResolution(): Boolean {
    return initialLocalResolution == null || localResolution != null
  }

  private fun checkSettingsChanged(): Boolean {
    return isAutoResizeEnabled != initialAutoResizeEnabled ||
           localResolution != initialLocalResolution
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

  companion object {
    private const val TITLE = "Graphics settings"
    private const val CHECKBOX_TEXT = "Auto resize"
    private const val INVALID_INTEGER_INPUT_TEXT = "Expected positive integer"

    private fun String.toResolutionOrNull(): Int? {
      return toIntOrNull()?.takeIf { it > 0 }
    }
  }
}
