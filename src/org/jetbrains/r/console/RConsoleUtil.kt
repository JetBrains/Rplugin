/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.notifications.RNotificationUtil

object RConsoleUtil {
  private const val CONSOLE_GROUP_ID = "RConsole"

  fun notifyError(project: Project, message: String?) {
    RNotificationUtil.notifyError(project, CONSOLE_GROUP_ID, "R Console Failure", message)
  }
}
