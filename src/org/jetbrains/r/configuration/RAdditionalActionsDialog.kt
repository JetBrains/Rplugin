/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import icons.org.jetbrains.r.RBundle
import javax.swing.*

class RAdditionalActionsDialog(private val onExit: (Boolean, Boolean) -> Unit) : DialogWrapper(null, true) {
  private val closeCheckBox = JBCheckBox(CLOSE_OLD_CONSOLES_TEXT, false)
  private val openCheckBox = JBCheckBox(OPEN_NEW_CONSOLE_TEXT, true)

  init {
    title = TITLE
    init()
  }

  override fun createActions(): Array<Action> {
    // Suppress "Cancel" button
    return arrayOf(okAction)
  }

  override fun createCenterPanel(): JComponent? {
    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(JLabel(DESCRIPTION))
      add(closeCheckBox)
      add(openCheckBox)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onExit(closeCheckBox.isSelected, openCheckBox.isSelected)
  }

  companion object {
    private val TITLE = RBundle.message("project.settings.additional.actions.title")
    private val DESCRIPTION = RBundle.message("project.settings.additional.actions.description")
    private val CLOSE_OLD_CONSOLES_TEXT = RBundle.message("project.settings.additional.actions.close.old.consoles")
    private val OPEN_NEW_CONSOLE_TEXT = RBundle.message("project.settings.additional.actions.open.new.console")
  }
}
