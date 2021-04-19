package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.update.UpdateUtils.showSizeMb
import java.util.concurrent.atomic.AtomicReference


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  private val notificationsTitle = RBundle.message("project.settings.ml.completion.name")

  private val activeAskForUpdateNotification = AtomicReference<Notification?>(null)

  fun showPopup(project: Project?, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long): Unit =
    when {
      artifacts.any(MachineLearningCompletionRemoteArtifact::localIsMissing) -> askToDownload(project, artifacts, size)
      else -> askForUpdate(project, artifacts, size)
    }

  fun DialogWrapper.showUpdateDialog() {
    val agreedToUpdate = showAndGet()
    if (agreedToUpdate) {
      activeAskForUpdateNotification.get()?.expire()
    }
  }

  private fun createBasicUpdateNotification(project: Project?,
                                            artifacts: List<MachineLearningCompletionRemoteArtifact>,
                                            text: String,
                                            actionText: String): Notification =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, text)
      .addAction(object : NotificationAction(actionText) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          MachineLearningCompletionUpdateAction(project, artifacts).performAsync()
          notification.expire()
        }
      })

  private fun Notification.tryNotifyAndSetAsActive(project: Project?): Unit =
    activeAskForUpdateNotification.get().let { oldNotification ->
      val swapped = activeAskForUpdateNotification.compareAndSet(oldNotification, this)
      if (!swapped) {
        return@let
      }

      oldNotification?.expire()
      if (MachineLearningCompletionUpdateAction.canInitiateUpdateAction.get()) {
        notify(project)
      }
    }

  private fun askToDownload(project: Project?, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long): Unit =
    createBasicUpdateNotification(project,
                                  artifacts,
                                  RBundle.message("notification.ml.update.askToDownload.content", showSizeMb(size)),
                                  RBundle.message("notification.ml.update.askToDownload.downloadButton"))
      .tryNotifyAndSetAsActive(project)

  private fun askForUpdate(project: Project?, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long): Unit =
    createBasicUpdateNotification(project,
                                  artifacts,
                                  RBundle.message("notification.ml.update.askForUpdate.content", showSizeMb(size)),
                                  RBundle.message("notification.ml.update.askForUpdate.updateButton"))
      .addAction(object : NotificationAction(RBundle.message("notification.ml.update.askForUpdate.ignoreButton")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          artifacts.forEach { it.ignoreLatestVersion() }
          notification.expire()
        }
      })
      .tryNotifyAndSetAsActive(project)

  fun notifyUpdateCompleted(project: Project?): Unit =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateCompleted.content"))
      .notify(project)

  fun notifyUpdateFailed(project: Project?, artifact: MachineLearningCompletionArtifact): Unit =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateFailed.content", artifact.visibleName))
      .notify(project)
}
