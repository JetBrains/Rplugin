/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.DialogUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.settings.RInterpreterSettingsProvider
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

  private lateinit var component: JComponent

  init {
    title = TITLE
    currentInterpreters.addAll(interpreters)
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val decorator = ToolbarDecorator.createDecorator(interpreterList).apply {
      disableUpDownActions()
      setAddAction { button ->
        val onAdded = { info: RInterpreterInfo ->
          currentInterpreters.add(info)
          refresh(true)
          currentSelection = info
        }
        if (RInterpreterSettingsProvider.getProviders().size > 1) {
          val popupPoint = button.preferredPopupPoint?.screenPoint ?: button.contextComponent.locationOnScreen
          RInterpreterDetailsStep.show(currentInterpreters, null, component, popupPoint, onAdded)
        } else {
          addInterpreter(onAdded)
        }
      }
      setRemoveAction {
        removeInterpreter()
      }

      if (RInterpreterSettingsProvider.getProviders().any { it.isEditingSupported() }) {
        setEditAction {
          val selection = currentSelection ?: return@setEditAction
          val index = currentInterpreters.indexOf(selection).takeIf { it >= 0 } ?: return@setEditAction
          RInterpreterSettingsProvider.getProviders()
            .firstOrNull { it.canEditInterpreter(selection) }
            ?.showEditInterpreterDialog(selection, currentInterpreters) {
              currentInterpreters[index] = it
              refresh(true)
              currentSelection = it
            }
        }
        setEditActionUpdater {
          val selection = currentSelection
          selection != null && RInterpreterSettingsProvider.getProviders().any { provider -> provider.canEditInterpreter(selection) }
        }
      }

      setPreferredSize(DialogUtil.calculatePreferredSize(DialogUtil.SizePreference.NARROW, DialogUtil.SizePreference.MODERATE))
    }
    refresh(false)
    currentSelection = initialSelection
    return decorator.createPanel().also { component = it }
  }

  override fun doOKAction() {
    onModified(currentInterpreters, currentSelection)
    super.doOKAction()
  }

  private fun addInterpreter(onAdded: (RInterpreterInfo) -> Unit) {
    RAddInterpreterDialog.show(currentInterpreters, onAdded)
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
