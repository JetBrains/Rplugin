/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.util.ui.SwingHelper

object RActionUtil {

  /**
   * used for triggering statistic recording on understandable action id
   */
  fun fireBeforeActionById(id: String, place: String = ActionPlaces.UNKNOWN) {
    val actionManager = ActionManagerEx.getInstanceEx()
    val action: AnAction = actionManager.getAction(id)
    val dataContext = DataManager.getInstance().getDataContext(SwingHelper.getComponentFromRecentMouseEvent())
    val event = AnActionEvent.createFromAnAction(action, null, place, dataContext)
    actionManager.fireBeforeActionPerformed(action, dataContext, event)
  }

  fun executeActionById(actionId: String, project: Project) {
    val action = ActionManager.getInstance().getAction(actionId) ?: throw IllegalStateException("No action ")
    invokeLater {
      DataManager.getInstance().dataContextFromFocusAsync.onSuccess { dataContext ->
        TransactionGuard.submitTransaction(project, Runnable {
          val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)
          ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext)
        })
      }
    }
  }
}