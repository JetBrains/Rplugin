package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt.MEGABYTE
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean


object MachineLearningCompletionNotifications {

  const val GROUP_NAME = "RMachineLearningCompletion"

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
          val numberOfTasks = artifacts.size

          val releaseFlagCallback = TaskUtils.createSharedCallback(numberOfTasks) {
            MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
          }

          val notifyUpdateCompletedCallback = TaskUtils.createSharedCallback(numberOfTasks) {
            notifyUpdateCompleted(project)
          }

          val localServerDirectory = File(MachineLearningCompletionModelFilesService.getInstance().localServerDirectory!!)
          artifacts.forEach { artifact ->
            val artifactName = artifact.visibleName
            val artifactTempFile = File.createTempFile(artifact.id, ".zip", localServerDirectory)

            val unzipTaskTitle = RBundle.message("rmlcompletion.task.unzip", artifactName)
            val unzipTask =
              object : MachineLearningCompletionModelFilesService.UpdateArtifactTask(artifact, artifactTempFile, project, unzipTaskTitle) {
                override fun onSuccess() = notifyUpdateCompletedCallback()

                override fun onFinished() = releaseFlagCallback()
              }

            val downloadTaskTitle = RBundle.message("rmlcompletion.task.download", artifactName)
            val downloadTask =
              object : MachineLearningCompletionDownloadModelService.DownloadArtifactTask(artifact, artifactTempFile, project,
                                                                                          downloadTaskTitle) {
                override fun onSuccess() = unzipTask.queue()
              }

            downloadTask.queue()
          }

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
