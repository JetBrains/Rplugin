/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.interpreter

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.interpreter.RInterpreterLocation
import java.awt.BorderLayout
import javax.swing.ButtonGroup
import javax.swing.Icon
import javax.swing.JPanel

class RChooseInterpreterGroupPanel(name: String,
                                   panelIcon: Icon,
                                   private val panels: List<RInterpreterPanel>,
                                   defaultPanel: RInterpreterPanel) : RInterpreterPanel() {
  override val panelName: String = name
  override val icon: Icon = panelIcon
  var mySelectedPanel: RInterpreterPanel = defaultPanel

  init {
    layout = BorderLayout()
    val contentPanel = if (panels.size == 1) {
      panels[0]
    }
    else {
      createRadioButtonPanel(panels, defaultPanel)
    }
    add(contentPanel, BorderLayout.NORTH)
  }

  override fun validateInterpreter(): List<ValidationInfo> = flatMapEnabledPanels { validateInterpreter() }

  override fun fetchInstalledPackages(): List<String> = flatMapEnabledPanels { fetchInstalledPackages() }

  private fun <T> flatMapEnabledPanels(mapFun: RInterpreterPanel.() -> List<T>): List<T> {
    return panels.filter { it.isEnabled }.flatMap { it.mapFun() }
  }

  override val interpreterLocation: RInterpreterLocation?
    get() = mySelectedPanel.interpreterLocation

  override fun addChangeListener(listener: Runnable) {
    changeListeners += listener
    for (panel in panels) {
      panel.addChangeListener(listener)
    }
  }

  private fun createRadioButtonPanel(panels: List<RInterpreterPanel>, defaultPanel: RInterpreterPanel): JPanel {
    val buttonMap = panels.map { JBRadioButton(it.panelName) to it }.toMap(linkedMapOf())
    ButtonGroup().apply {
      buttonMap.keys.forEach { add(it) }
    }

    val formBuilder = FormBuilder.createFormBuilder()
    for ((button, panel) in buttonMap) {
      panel.border = JBUI.Borders.emptyLeft(30)
      formBuilder.addComponent(button)
      formBuilder.addComponent(panel)
      button.addItemListener {
        for (c in panels) {
          UIUtil.setEnabled(c, c == panel, true)
        }
        if (button.isSelected) {
          mySelectedPanel = panel
        }
        runListeners()
      }
    }

    buttonMap.filterValues { it == defaultPanel }.keys.first().isSelected = true
    return formBuilder.panel
  }
}