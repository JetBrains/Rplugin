package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt.MEGABYTE
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.nio.file.Files
import java.nio.file.Path
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
          val serverService = MachineLearningCompletionServerService.getInstance()
          serverService.shutdownBlocking()

          val numberOfTasks = artifacts.size
          val releaseFlagCallback = TaskUtils.createSharedCallback(numberOfTasks) {
            MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
          }

          val updateCompletedCallback = TaskUtils.createSharedCallback(numberOfTasks) {
            notifyUpdateCompleted(project)
            serverService.tryRelaunchServer()
          }

          val localServerDirectory = Path.of(MachineLearningCompletionModelFilesService.getInstance().localServerDirectory!!)
          artifacts.forEach { artifact ->
            val artifactTempFile = Files.createTempFile(localServerDirectory, artifact.id, ".zip")

            val unzipTask = createUnzipTask(artifact, artifactTempFile, project, updateCompletedCallback, releaseFlagCallback)

            val downloadTask = createDownloadTask(artifact, artifactTempFile, project, unzipTask, releaseFlagCallback)

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

  private fun createUnzipTask(artifact: MachineLearningCompletionRemoteArtifact,
                              artifactTempFile: Path,
                              project: Project,
                              updateCompletedCallback: () -> Unit,
                              releaseFlagCallback: () -> Unit): MachineLearningCompletionModelFilesService.UpdateArtifactTask {
    val artifactName = artifact.visibleName
    val unzipTaskTitle = RBundle.message("rmlcompletion.task.unzip", artifactName)
    return object : MachineLearningCompletionModelFilesService.UpdateArtifactTask(artifact, artifactTempFile, project, unzipTaskTitle) {
      override fun onSuccess() {
        updateCompletedCallback()
        releaseFlagCallback()
      }

      override fun onThrowable(error: Throwable) {
        notifyUpdateFailed(project, artifact)
        releaseFlagCallback()
      }

      override fun onCancel() = releaseFlagCallback()
    }
  }

  private fun createDownloadTask(artifact: MachineLearningCompletionRemoteArtifact,
                                 artifactTempFile: Path,
                                 project: Project,
                                 unzipTask: MachineLearningCompletionModelFilesService.UpdateArtifactTask,
                                 releaseFlagCallback: () -> Unit): MachineLearningCompletionDownloadModelService.DownloadArtifactTask {
    val artifactName = artifact.visibleName
    val downloadTaskTitle = RBundle.message("rmlcompletion.task.download", artifactName)
    return object : MachineLearningCompletionDownloadModelService.DownloadArtifactTask(artifact, artifactTempFile, project,
                                                                                       downloadTaskTitle) {
      override fun onSuccess() = unzipTask.queue()

      override fun onThrowable(error: Throwable) {
        notifyUpdateFailed(project, artifact)
        releaseFlagCallback()
      }

      override fun onCancel() = releaseFlagCallback()
    }
  }

  fun notifyUpdateCompleted(project: Project) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateCompleted.content"))
      .notify(project)

  fun notifyUpdateFailed(project: Project, artifact: MachineLearningCompletionRemoteArtifact) =
    NotificationGroupManager.getInstance().getNotificationGroup(GROUP_NAME)
      .createNotification(notificationsTitle, RBundle.message("notification.ml.update.updateFailed.content", artifact.visibleName))
      .notify(project)
}
