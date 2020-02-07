/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.sdk.RInterpreterListCellRenderer
import org.jetbrains.r.ui.RDimensionPreference
import org.jetbrains.r.ui.calculateDialogPreferredSize
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class RInterpreterDetailsDialog(
  private val initialSelection: RInterpreterInfo?,
  interpreters: List<RInterpreterInfo>,
  private val onModified: (List<RInterpreterInfo>, RInterpreterInfo?) -> Unit
) : DialogWrapper(null, true) {

  private val interpreterList = JBList<RInterpreterInfo>().apply {
    cellRenderer = RInterpreterListCellRenderer()
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    addListSelectionListener {
      refreshOkButton()
    }
  }

  private val currentInterpreters = mutableListOf<RInterpreterInfo>()

  private var currentSelection: RInterpreterInfo?
    get() = interpreterList.selectedValue
    set(value) {
      interpreterList.setSelectedValue(value, true)
    }

  private var isModified = false

  init {
    title = TITLE
    currentInterpreters.addAll(interpreters)
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val decorator = ToolbarDecorator.createDecorator(interpreterList).apply {
      disableUpDownActions()
      setAddAction {
        addInterpreter()
      }
      setRemoveAction {
        removeInterpreter()
      }
      setPreferredSize(calculateDialogPreferredSize(RDimensionPreference.NARROW, RDimensionPreference.MODERATE))
    }
    refresh(false)
    currentSelection = initialSelection
    return decorator.createPanel()
  }

  override fun doOKAction() {
    onModified(currentInterpreters, currentSelection)
    super.doOKAction()
  }

  private fun addInterpreter() {
    RAddInterpreterDialog.show(currentInterpreters) {
      currentInterpreters.add(it)
      refresh(true)
      currentSelection = it
    }
  }

  private fun removeInterpreter() {
    interpreterList.selectedValue?.let {
      currentInterpreters.remove(it)
      refresh(true)
      currentSelection = currentInterpreters.firstOrNull()
    }
  }

  private fun refresh(isModified: Boolean) {
    this.isModified = isModified
    refreshInterpreterList()
    refreshOkButton()
  }

  private fun refreshOkButton() {
    isOKActionEnabled = isModified()
  }

  private fun isModified(): Boolean {
    return isModified || currentSelection !== initialSelection
  }

  private fun refreshInterpreterList() {
    interpreterList.model = CollectionListModel(currentInterpreters)
  }

  companion object {
    private val TITLE = RBundle.message("project.settings.details.dialog.title")
  }
}
