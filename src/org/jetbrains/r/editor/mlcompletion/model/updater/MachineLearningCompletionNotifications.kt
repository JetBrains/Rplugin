package org.jetbrains.r.editor.mlcompletion

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDownloadModelService


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  // TODO: Add estimated size of the download
  fun askForUpdate(project: Project, size: Int) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification("R Machine Learning completion update is available",
                          "Size: $size Mb") // Dont ask, just give an update button
      .addAction(object : NotificationAction("Update") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          service<MachineLearningCompletionDownloadModelService>()
          notification.expire()
        }
      })
      .notify(project)


  fun notifyUpdateCompleted(project: Project) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification("R Machine Learning completion has been successfully updated", "")
      .notify(project)

}
