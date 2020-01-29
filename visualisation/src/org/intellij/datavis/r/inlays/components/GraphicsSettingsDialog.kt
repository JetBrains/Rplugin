/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.ui.DialogWrapper
import org.intellij.datavis.r.inlays.components.forms.GraphicsSettingsDialogForm
import javax.swing.JComponent

class GraphicsSettingsDialog(
  private val initialAutoResizeEnabled: Boolean,
  private val onSettingsChange: (Boolean) -> Unit
) : DialogWrapper(null, true) {

  private val form = GraphicsSettingsDialogForm()

  private val isAutoResizeEnabled: Boolean
    get() = form.autoResizeCheckBox.isSelected

  init {
    title = TITLE
    init()
    form.autoResizeCheckBox.apply {
      isSelected = initialAutoResizeEnabled
      addItemListener {
        updateOkAction()
      }
    }
    updateOkAction()
  }

  override fun createCenterPanel(): JComponent? {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    onSettingsChange(isAutoResizeEnabled)
  }

  private fun updateOkAction() {
    isOKActionEnabled = isAutoResizeEnabled != initialAutoResizeEnabled
  }

  companion object {
    private const val TITLE = "Graphics settings"
    private const val CHECKBOX_TEXT = "Auto resize"
  }
}
