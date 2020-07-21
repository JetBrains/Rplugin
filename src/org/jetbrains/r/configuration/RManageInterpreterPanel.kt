/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.FixedSizeButton
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.util.ui.JBUI
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.sdk.RInterpreterDetailsStep
import org.jetbrains.r.sdk.RInterpreterListCellRenderer
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class RManageInterpreterPanel(text: String, private val addOnly: Boolean, private val onSelected: (() -> Unit)?) {
  private val panel = JPanel(GridBagLayout())

  private val comboBox = ComboBox<RInterpreterInfo>().apply {
    renderer = RInterpreterListCellRenderer()
    addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        currentSelection = selectedItem as RInterpreterInfo?
        isModified = true
        onSelected?.invoke()
      }
    }
  }
  private val editButton = FixedSizeButton().apply {
    icon = AllIcons.Actions.Edit
    val preferredHeight = comboBox.height
    preferredSize = Dimension(preferredHeight, preferredHeight)
    addActionListener {
      val selection = currentSelection ?: return@addActionListener
      val index = currentInterpreters.indexOf(selection).takeIf { it >= 0 } ?: return@addActionListener
      RInterpreterSettingsProvider.getProviders()
        .firstOrNull { it.canEditInterpreter(selection) }
        ?.showEditInterpreterDialog(selection, currentInterpreters) {
          currentInterpreters[index] = it
          changeSelection(it)
        }
    }
  }

  private var isModified = false

  val component: JComponent = panel
  val currentInterpreters = mutableListOf<RInterpreterInfo>()

  var initialInterpreters: List<RInterpreterInfo> = listOf()
  var initialSelection: RInterpreterInfo? = null

  var currentSelection: RInterpreterInfo? = null
    private set(value) {
      field = value
      editButton.isEnabled =
        value != null && RInterpreterSettingsProvider.getProviders().any { it.canEditInterpreter(value) }
    }

  init {
    fun createLabel() = JLabel(text)

    fun createDetailsButton(preferredHeight: Int) = FixedSizeButton().apply {
      icon = if (addOnly) AllIcons.General.Add else AllIcons.General.GearPlain
      preferredSize = Dimension(preferredHeight, preferredHeight)
      addActionListener {
        if (!addOnly) {
          val allDialog = RInterpreterDetailsDialog(currentSelection, currentInterpreters) { interpreters, selection ->
            modifyInterpreters(interpreters, selection)
          }
          RInterpreterDetailsStep.show(currentInterpreters, allDialog, panel, this.locationOnScreen) { interpreter ->
            addInterpreter(interpreter)
          }
        } else {
          RAddInterpreterDialog.show(currentInterpreters) { interpreter ->
            addInterpreter(interpreter)
          }
        }
      }
    }

    addComponentToPanel(createLabel(), 0, 0)
    addComponentToPanel(comboBox, 1, 0, 0.1)
    addComponentToPanel(createDetailsButton(comboBox.height), 2, 0, 0.0)
    if (!addOnly) addComponentToPanel(editButton, 3, 0, 0.0)
  }

  private fun addComponentToPanel(
    component: JComponent,
    xGrid: Int,
    yGrid: Int,
    xWeight: Double? = null
  ) {
    val constraints = GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      insets = JBUI.insets(2)
      gridx = xGrid
      gridy = yGrid
      if (xWeight != null) {
        weightx = xWeight
      }
    }
    panel.add(component, constraints)
  }

  private fun addInterpreter(interpreter: RInterpreterInfo) {
    currentInterpreters.add(interpreter)
    changeSelection(interpreter)
  }

  private fun modifyInterpreters(interpreters: List<RInterpreterInfo>, selection: RInterpreterInfo?) {
    currentInterpreters.clear()
    currentInterpreters.addAll(interpreters)
    changeSelection(selection)
  }

  private fun changeSelection(selection: RInterpreterInfo?) {
    currentSelection = selection
    refresh()
    onSelected?.invoke()
  }

  private fun refresh() {
    this.isModified = true
    if (isModified) {
      refreshComboBox()
    }
  }

  private fun refreshComboBox() {
    val interpreters = listOf(null, RInterpreterListCellRenderer.SEPARATOR) + currentInterpreters
    comboBox.model = object : CollectionComboBoxModel<RInterpreterInfo>(interpreters, currentSelection) {
      override fun setSelectedItem(item: Any?) {
        if (item !== RInterpreterListCellRenderer.SEPARATOR) {
          super.setSelectedItem(item)
        }
      }
    }
  }

  fun isModified(): Boolean {
    return isModified
  }

  fun reset() {
    isModified = false
    currentSelection = initialSelection
    currentInterpreters.clear()
    currentInterpreters.addAll(initialInterpreters)
    refreshComboBox()
  }
}
