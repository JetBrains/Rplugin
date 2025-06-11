package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon

abstract class RDumbAwareEdtToggleAction : DumbAwareToggleAction {
  constructor(@NlsActions.ActionText text: String?,
              @NlsActions.ActionDescription description: String?,
              icon: Icon?) : super(text, description, icon)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}