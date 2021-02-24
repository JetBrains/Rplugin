package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.model.updater.UpdateUtils.showSizeMb


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  private val notificationsTitle = RBundle.message("project.settings.ml.completion.name")

  @Volatile
  var activeAskForUpdateNotification: Notification? = null
    private set

  fun showPopup(project: Project, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long) =
    when {
      artifacts.any(MachineLearningCompletionRemoteArtifact::localIsMissing) -> askToDownload(project, artifacts, size)
      else -> askForUpdate(project, artifacts, size)
    }

  private fun createBasicUpdateNotification(project: Project,
                                            artifacts: List<MachineLearningCompletionRemoteArtifact>,
                                            text: String,
                                            actionText: String) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, text)
      .addAction(object : NotificationAction(actionText) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          MachineLearningCompletionUpdateAction(project, artifacts).performAsync()
          notification.expire()
        }
      })

  private fun Notification.notifyAndSetAsActive(project: Project) =
    also { activeAskForUpdateNotification = it }.notify(project)

  private fun askToDownload(project: Project, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long) =
    createBasicUpdateNotification(project,
                                  artifacts,
                                  RBundle.message("notification.ml.update.askToDownload.content", showSizeMb(size)),
                                  RBundle.message("notification.ml.update.askToDownload.downloadButton"))
      .notifyAndSetAsActive(project)

  private fun askForUpdate(project: Project, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long) =
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
      .notifyAndSetAsActive(project)

  fun notifyUpdateCompleted(project: Project?) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateCompleted.content"))
      .notify(project)

  fun notifyUpdateFailed(project: Project?, artifact: MachineLearningCompletionRemoteArtifact) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateFailed.content", artifact.visibleName))
      .notify(project)
}
