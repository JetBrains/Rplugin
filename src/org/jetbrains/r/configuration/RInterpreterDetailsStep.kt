/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import java.awt.Point
import javax.swing.JComponent

class RInterpreterDetailsStep(
  private val existingInterpreters: List<RInterpreterInfo>,
  private val allDialog: DialogWrapper?,
  private val onAdded: (RInterpreterInfo) -> Unit
) : BaseListPopupStep<String>(null, getEntries(allDialog != null)) {

  override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
    return doFinalStep {
      onSelection(selectedValue)
    }
  }

  private fun onSelection(selectedValue: String?) {
    if (selectedValue == SHOW_ALL) allDialog?.show()
    RInterpreterSettingsProvider.getProviders().forEach {
      if (it.getAddInterpreterActionName() == selectedValue) {
        it.showAddInterpreterDialog(existingInterpreters, onAdded)
        return
      }
    }
  }

  companion object {
    private val SHOW_ALL = RBundle.message("project.settings.details.step.show.all")

    private fun getEntries(withShowAll: Boolean): List<String> {
      return RInterpreterSettingsProvider.getProviders().map { it.getAddInterpreterActionName() }
        .let {
          if (withShowAll) {
            it.plus(SHOW_ALL)
          } else {
            it
          }
        }
    }

    fun show(
      existingInterpreters: List<RInterpreterInfo>,
      allDialog: DialogWrapper?,
      owner: JComponent,
      point: Point,
      onAdded: (RInterpreterInfo) -> Unit
    ) {
      val step = RInterpreterDetailsStep(existingInterpreters, allDialog, onAdded)
      val popup = JBPopupFactory.getInstance().createListPopup(step)
      popup.showInScreenCoordinates(owner, point)
    }
  }
}
