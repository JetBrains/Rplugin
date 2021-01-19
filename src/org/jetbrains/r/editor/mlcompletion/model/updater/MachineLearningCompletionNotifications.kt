package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt.MEGABYTE
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

  private val downloadService = MachineLearningCompletionDownloadModelService.getInstance()

  private val notificationsTitle = RBundle.message("notification.ml.title")

  private val sizeFormat = DecimalFormat("#.#")
  private fun showSizeMb(sizeBytes: Long) = sizeFormat.format(sizeBytes / MEGABYTE.toDouble())

  fun askForUpdate(project: Project, artifacts: List<MachineLearningCompletionRemoteArtifact>, size: Long) {
    val updateIsInitiated = AtomicBoolean(false)
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.askForUpdate.content", showSizeMb(size)))
      .addAction(object : NotificationAction(RBundle.message("notification.ml.update.askForUpdate.updateButton")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          updateIsInitiated.set(true)
          val releaseFlagCallback = {
            MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
          }
          val filesService = MachineLearningCompletionModelFilesService.getInstance()
          val unzipTask = object : Task.Backgroundable(project, "unzip") {
            override fun run(indicator: ProgressIndicator) = artifacts.forEach { artifact ->
              filesService.updateArtifacts(indicator, artifacts)
            }

            override fun onSuccess() {
              notifyUpdateCompleted(project)
            }

            override fun onFinished() = releaseFlagCallback()
          }

          downloadService.createDownloadAndUpdateTask(project,
                                                      artifacts,
                                                      onSuccessCallback = { unzipTask.queue() },
                                                      onFinishedCallback = { }).queue()
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
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateCompleted.content"))
      .notify(project)

}
