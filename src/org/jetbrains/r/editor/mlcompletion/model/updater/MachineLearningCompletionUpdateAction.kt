package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class MachineLearningCompletionUpdateAction(val project: Project?,
                                            val artifacts: List<MachineLearningCompletionRemoteArtifact>) {

  private val updateIsInitiated = AtomicBoolean(false)

  fun isInitiated() = updateIsInitiated.get()

  fun performAsync() {
    if (!updateIsInitiated.compareAndSet(false, true)) {
      // TODO: maybe throw exception or smth like that to warn user that he tries to perform a second time
      return
    }

    val serverService = MachineLearningCompletionServerService.getInstance()
    serverService.shutdownBlocking()

    val numberOfTasks = artifacts.size
    val releaseFlagCallback = TaskUtils.createSharedCallback(numberOfTasks) {
      MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
    }

    val updateCompletedCallback = TaskUtils.createSharedCallback(numberOfTasks) {
      MachineLearningCompletionNotifications.notifyUpdateCompleted(project)
      serverService.tryRelaunchServer()
    }

    val localServerDirectory = Path.of(MachineLearningCompletionModelFilesService.getInstance().localServerDirectory!!)
    artifacts.forEach { artifact ->
      val artifactTempFile = Files.createTempFile(localServerDirectory, artifact.id, ".zip")

      val unzipTask = createUnzipTask(artifact, artifactTempFile, project, updateCompletedCallback,
                                      releaseFlagCallback)

      val downloadTask = createDownloadTask(artifact, artifactTempFile, project, unzipTask,
                                            releaseFlagCallback)

      downloadTask.queue()
    }
  }

  private fun createUnzipTask(artifact: MachineLearningCompletionRemoteArtifact,
                              artifactTempFile: Path,
                              project: Project?,
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
        MachineLearningCompletionNotifications.notifyUpdateFailed(project, artifact)
        releaseFlagCallback()
      }

      override fun onCancel() = releaseFlagCallback()
    }
  }

  private fun createDownloadTask(artifact: MachineLearningCompletionRemoteArtifact,
                                 artifactTempFile: Path,
                                 project: Project?,
                                 unzipTask: MachineLearningCompletionModelFilesService.UpdateArtifactTask,
                                 releaseFlagCallback: () -> Unit): MachineLearningCompletionDownloadModelService.DownloadArtifactTask {
    val artifactName = artifact.visibleName
    val downloadTaskTitle = RBundle.message("rmlcompletion.task.download", artifactName)
    return object : MachineLearningCompletionDownloadModelService.DownloadArtifactTask(artifact, artifactTempFile, project,
                                                                                       downloadTaskTitle) {
      override fun onSuccess() = unzipTask.queue()

      override fun onThrowable(error: Throwable) {
        MachineLearningCompletionNotifications.notifyUpdateFailed(project, artifact)
        releaseFlagCallback()
      }

      override fun onCancel() = releaseFlagCallback()
    }
  }
}
