// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.RFileType

class RConsoleAction : AnAction(), DumbAware {
  init {
    templatePresentation.icon = RFileType.icon
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = CommonDataKeys.PROJECT.getData(event.dataContext)
    if (project != null) {
      RConsoleManager.runConsole(project, requestFocus = true).onError { e ->
        RNotificationUtil.notifyConsoleError(project, e.message)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = CommonDataKeys.PROJECT.getData(e.dataContext)
    e.presentation.isVisible = project != null
  }
}
