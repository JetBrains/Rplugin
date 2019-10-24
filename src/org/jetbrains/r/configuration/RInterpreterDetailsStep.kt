/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.sdk

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.configuration.RAddInterpreterDialog
import org.jetbrains.r.interpreter.RInterpreterInfo
import java.awt.Point
import javax.swing.JComponent

class RInterpreterDetailsStep(
  private val existingInterpreters: List<RInterpreterInfo>,
  private val allDialog: DialogWrapper,
  private val onAdded: (RInterpreterInfo) -> Unit
) : BaseListPopupStep<String>(null, listOf(ADD, SHOW_ALL)) {

  override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
    return doFinalStep {
      onSelection(selectedValue)
    }
  }

  private fun onSelection(selectedValue: String?) {
    when (selectedValue) {
      ADD -> showAddDialog()
      SHOW_ALL -> showAllDialog()
    }
  }

  private fun showAddDialog() {
    RAddInterpreterDialog.show(existingInterpreters, onAdded)
  }

  private fun showAllDialog() {
    allDialog.show()
  }

  companion object {
    private val ADD = RBundle.message("project.settings.details.step.add")
    private val SHOW_ALL = RBundle.message("project.settings.details.step.show.all")

    fun show(
      existingInterpreters: List<RInterpreterInfo>,
      allDialog: DialogWrapper,
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
