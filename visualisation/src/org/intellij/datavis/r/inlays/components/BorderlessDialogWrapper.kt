/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.JBUI
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BorderlessDialogWrapper(project: Project, private val dialogTitle: String, ideModalityType: IdeModalityType) :
  DialogWrapper(project, null, true, ideModalityType, false)
{
  override fun init() {
    super.init()
    title = dialogTitle
    removeMarginsIfPossible()
  }

  override fun createActions(): Array<Action> {
    return if (SystemInfo.isMac) arrayOf(cancelAction, okAction) else arrayOf(okAction, cancelAction)
  }

  protected fun createOkCancelPanel(): JComponent {
    val buttons = createActions().map { createJButtonForAction(it) }
    return createButtonsPanel(buttons)
  }

  private fun removeMarginsIfPossible() {
    (rootPane.contentPane as? JPanel)?.let { panel ->
      panel.border = JBUI.Borders.empty()
    }
  }
}
