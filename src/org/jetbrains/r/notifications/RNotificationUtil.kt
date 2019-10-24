/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object RNotificationUtil {
  fun notifyError(project: Project, groupDisplayId: String, title: String, message: String?) {
    val notification = Notification(groupDisplayId, title, message ?: "Unknown error", NotificationType.ERROR)
    notification.notify(project)
  }
}
