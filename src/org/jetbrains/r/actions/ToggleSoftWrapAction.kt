/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

abstract class ToggleSoftWrapAction : ToggleAction(ActionsBundle.actionText("EditorToggleUseSoftWraps"),
                                                   ActionsBundle.actionDescription("EditorToggleUseSoftWraps"),
                                                   AllIcons.Actions.ToggleSoftWrap), DumbAware {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}