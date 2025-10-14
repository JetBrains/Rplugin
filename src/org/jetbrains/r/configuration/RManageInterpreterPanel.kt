/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.FixedSizeButton
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.Nls
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RLocalInterpreterSettingsProvider
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class RManageInterpreterPanel(@Nls text: String, private val localOnly: Boolean, private val onSelected: (() -> Unit)?) {
  private val panel = JPanel(GridBagLayout())

  private val comboBox = ComboBox<Any>().apply {
    renderer = RInterpreterListCellRenderer()
    addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        currentSelection = selectedItem as RInterpreterInfo?
        isModified = true
        onSelected?.invoke()
      }
    }
  }

  private var isModified = false

  val component: JComponent = panel
  val currentInterpreters = mutableListOf<RInterpreterInfo>()

  var initialInterpreters: List<RInterpreterInfo> = listOf()
  var initialSelection: RInterpreterInfo? = null

  var currentSelection: RInterpreterInfo? = null
    private set

  init {
    fun createLabel() = JLabel(text)

    fun createDetailsButton(preferredHeight: Int) = FixedSizeButton().apply {
      icon = if (localOnly) AllIcons.General.Add else AllIcons.General.GearPlain
      preferredSize = Dimension(preferredHeight, preferredHeight)
      addActionListener {
        if (!localOnly) {
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
    val providers = if (localOnly) listOf(RLocalInterpreterSettingsProvider()) else RInterpreterSettingsProvider.getProviders ()
    val separator = RInterpreterListCellRenderer.SEPARATOR
    val interpreters =
      listOf(null) +
      currentInterpreters.groupBy { it.interpreterLocation.javaClass }.values.flatMap { listOf(separator) + it } +
      listOf(separator) +
      providers

    comboBox.model = object : CollectionComboBoxModel<Any>(interpreters.toMutableList(), currentSelection) {
      override fun setSelectedItem(item: Any?) {
        if (item is RInterpreterSettingsProvider){
          item.showAddInterpreterDialog(currentInterpreters) { interpreter ->
            addInterpreter(interpreter)
          }
        } else if (item !== separator) {
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
