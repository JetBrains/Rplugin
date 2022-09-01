package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import java.util.function.Supplier
import javax.swing.Icon

abstract class RDumbAwareBgtAction : DumbAwareAction {
  constructor() : super()

  constructor(@NlsActions.ActionText text: String?,
              @NlsActions.ActionDescription description: String?,
              icon: Icon?) : super(text, description, icon)

  constructor(dynamicText: Supplier<@NlsActions.ActionText String>,
              dynamicDescription: Supplier<@NlsActions.ActionDescription String>,
              icon: Icon?) : super(dynamicText, dynamicDescription, icon)

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}