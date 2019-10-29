/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle

object RNotificationUtil {
  private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")

  fun notifyError(project: Project, groupDisplayId: String, title: String, message: String?, action: AnAction? = null) {
    val notification = Notification(groupDisplayId, title, message ?: UNKNOWN_ERROR_MESSAGE, NotificationType.ERROR)
    if (action != null) {
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
