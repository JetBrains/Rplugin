/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil

object RActionUtil {

  /**
   * fire before action performed event for statistics and perform [action]
   */
  fun performDelegatedAction(anAction: AnAction, actionEvent: AnActionEvent) {
    ActionUtil.performActionDumbAwareWithCallbacks(anAction, actionEvent)
  }
}