package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.nio.file.Files
import java.nio.file.Path

class MachineLearningCompletionUpdateAction(val project: Project?,
                                            val artifacts: List<MachineLearningCompletionRemoteArtifact>) {

  companion object {
    val canInitiateUpdateAction = AtomicBooleanProperty(true)
  }

  fun performAsync() {
    if (!canInitiateUpdateAction.compareAndSet(true, false)) {
      return
    }

    val serverService = MachineLearningCompletionServerService.getInstance()
    serverService.shutdownBlocking()

    val numberOfTasks = artifacts.size
    val releaseFlagCallback = UpdateUtils.createSharedCallback(numberOfTasks) {
      canInitiateUpdateAction.set(true)
    }

    val updateCompletedCallback = UpdateUtils.createSharedCallback(numberOfTasks) {
      MachineLearningCompletionNotifications.notifyUpdateCompleted(project)
      serverService.tryRelaunchServer()
    }

    val localServerDirectory = Path.of(MachineLearningCompletionModelFilesService.getInstance().localServerDirectory!!)
    artifacts.forEach { artifact ->
      val artifactTempFile = Files.createTempFile(localServerDirectory, artifact.id, ".zip")

      val unzipTask = createUnzipTask(artifact.localDelegate, artifactTempFile, project, updateCompletedCallback,
                                      releaseFlagCallback)

      val downloadTask = createDownloadTask(artifact, artifactTempFile, project, unzipTask,
                                            releaseFlagCallback)

      downloadTask.queue()
    }
  }

  private fun createUnzipTask(artifact: MachineLearningCompletionLocalArtifact,
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

      override fun onCancel(): Unit = releaseFlagCallback()
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
      override fun onSuccess(): Unit = unzipTask.queue()

      override fun onThrowable(error: Throwable) {
        MachineLearningCompletionNotifications.notifyUpdateFailed(project, artifact)
        releaseFlagCallback()
      }

      override fun onCancel(): Unit = releaseFlagCallback()
    }
  }
}
