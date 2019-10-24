/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.TransactionGuard
import org.jetbrains.r.RUsefulTestCase

abstract class EditorActionTest : RUsefulTestCase() {
  protected fun doActionTest(expected: String, actionId: String) {
    val action = ActionManager.getInstance().getAction(actionId) ?: error("Action $actionId is non found")
    DataManager.getInstance().dataContextFromFocusAsync.onSuccess { dataContext ->
      TransactionGuard.submitTransaction(project, Runnable {
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)
        ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext)
      })
    }
    myFixture.checkResult(expected)
  }
}