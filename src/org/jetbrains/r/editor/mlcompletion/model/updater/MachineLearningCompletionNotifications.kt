package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt.MEGABYTE
import org.jetbrains.r.RBundle
import java.text.DecimalFormat


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  private val notificationsTitle = RBundle.message("notification.ml.title")

  private val sizeFormat = DecimalFormat("#.#")
  private fun showSizeMb(sizeBytes: Long) = sizeFormat.format(sizeBytes / MEGABYTE.toDouble())

  fun askForUpdate(project: Project, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long) {
    val updateAction = MachineLearningCompletionUpdateAction(project, artifacts)
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.askForUpdate.content", showSizeMb(size)))
      .addAction(object : NotificationAction(RBundle.message("notification.ml.update.askForUpdate.updateButton")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          updateAction.performAsync()
          notification.expire()
        }
      })
      .whenExpired {
        if (!updateAction.isInitiated()) {
          MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
        }
      }
      .notify(project)
  }

  fun notifyUpdateCompleted(project: Project?) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateCompleted.content"))
      .notify(project)

  fun notifyUpdateFailed(project: Project?, artifact: MachineLearningCompletionRemoteArtifact) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateFailed.content", artifact.visibleName))
      .notify(project)
}
