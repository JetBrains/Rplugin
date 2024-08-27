/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle

object RNotificationUtil {
  private const val CONSOLE_GROUP_ID = "RConsole"
  private const val INTERPRETER_GROUP_ID = "RInterpreter"
  private const val GRAPHICS_GROUP_ID = "RGraphics"
  private const val EXECUTION_GROUP_ID = "Console Execution"

  fun notifyConsoleError(project: Project, message: String?, vararg actions: AnAction) {
    notifyError(project, CONSOLE_GROUP_ID, RBundle.message("notification.console.failure"), message, *actions)
  }

  fun notifyInterpreterError(project: Project, message: String?, vararg actions: AnAction) {
    notifyError(project, INTERPRETER_GROUP_ID, RBundle.message("notification.interpreter.failure"), message, *actions)
  }

  fun notifyGraphicsError(project: Project, message: String?, vararg actions: AnAction) {
    notifyError(project, GRAPHICS_GROUP_ID, RBundle.message("notification.graphics.failure"), message, *actions)
  }

  fun notifyExecutionError(project: Project, message: String?, vararg actions: AnAction) {
    notifyError(project, EXECUTION_GROUP_ID, RBundle.message("notification.execution.failure"), message, *actions)
  }

  private fun notifyError(project: Project, groupDisplayId: String, title: String, message: String?, vararg actions: AnAction) {
    val notification = Notification(groupDisplayId, title, message
                                                           ?: RBundle.message("notification.unknown.error.message"), NotificationType.ERROR)
    for (action in actions) {
      notification.addAction(action)
    }
    notification.notify(project)
  }

  fun createNotificationAction(text: String, onClick: () -> Unit): AnAction {
    return object : NotificationAction(text) {
      override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        notification.expire()
        onClick()
      }
    }
  }
}
