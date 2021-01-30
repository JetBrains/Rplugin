package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionAppArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionModelArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MachineLearningCompletionModelFilesService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionModelFilesService>()

    private inline fun <T> Lock.withTryLock(lockFailedValue: T, action: () -> T): T =
      if (tryLock()) {
        try {
          action()
        }
        finally {
          unlock()
        }
      }
      else {
        lockFailedValue
      }
  }

  private val modelLock = ReentrantLock()
  private val appLock = ReentrantLock()
  private fun MachineLearningCompletionRemoteArtifact.getLock() =
    when (this) {
      is MachineLearningCompletionAppArtifact -> appLock
      is MachineLearningCompletionModelArtifact -> modelLock
    }

  val localServerDirectory
    get() = MachineLearningCompletionModelFiles.localServerDirectory
  val modelVersion
    get() = modelLock.withLock { MachineLearningCompletionModelFiles.modelVersion }
  val applicationVersion
    get() = appLock.withLock { MachineLearningCompletionModelFiles.applicationVersion }

  open class UpdateArtifactTask(
    private val artifact: MachineLearningCompletionRemoteArtifact,
    private val artifactZipFile: Path,
    project: Project,
    progressTitle: String,
    private val deleteLocalFileOnFinish: Boolean = true,
    ): Task.Backgroundable(project, progressTitle, true) {

    override fun run(indicator: ProgressIndicator) = getInstance().updateArtifact(indicator, artifact, artifactZipFile.toFile())

    override fun onFinished() {
      if (deleteLocalFileOnFinish) {
        Files.delete(artifactZipFile)
      }
    }
  }

  fun updateArtifact(progress: ProgressIndicator, artifact: MachineLearningCompletionRemoteArtifact, zipFile: File) =
    artifact.getLock().withLock<Unit> {
      MachineLearningCompletionModelFiles.updateArtifactFromArchive(progress, artifact, zipFile)
    }

  fun tryRunActionOnFiles(action: (MachineLearningCompletionModelFiles) -> Unit): Boolean =
    appLock.withTryLock(false) {
      modelLock.withTryLock(false) {
        if (MachineLearningCompletionModelFiles.available()) {
          action(MachineLearningCompletionModelFiles)
          return true
        }
        return false
      }
    }
}
