// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.interpreter.RProfileErrorException
import com.intellij.r.psi.notifications.RNotificationUtil

class RConsoleAction : AnAction(), DumbAware {
  init {
    templatePresentation.icon = RFileType.icon
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.getData(CommonDataKeys.PROJECT)
    if (project != null) {
      RConsoleManagerImpl.runConsole(project, requestFocus = true).onError { e ->
        if (e !is RProfileErrorException) {
          RNotificationUtil.notifyConsoleError(project, e.message)
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    e.presentation.isVisible = project != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
