package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import java.util.concurrent.atomic.AtomicBoolean


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  private val downloadService = MachineLearningCompletionDownloadModelService.getInstance()

  private const val notificationsTitle = "R Machine Learning completion"

  fun askForUpdate(project: Project, descriptors: Collection<JpsMavenRepositoryLibraryDescriptor>) {
    val updateIsInitiated = AtomicBoolean(false)
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, "Update is available")
      .addAction(object : NotificationAction("Update") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          updateIsInitiated.set(true)
          downloadService.createDownloadAndUpdateTask(project,
                                                      descriptors,
                                                      onSuccessCallback = { notifyUpdateCompleted(project) },
                                                      onFinishedCallback = {
                                                        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
                                                      }).queue()
          notification.expire()
        }
      })
      .whenExpired {
        if (!updateIsInitiated.get()) {
          MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
        }
      }
      .notify(project)
  }

  fun notifyUpdateCompleted(project: Project) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, "Machine learning completion has been successfully updated")
      .notify(project)

}
