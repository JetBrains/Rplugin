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

    private inline fun <T> Lock.withTryLock(lockFailedValue: T, block: () -> T): T =
      if (tryLock()) {
        try {
          block()
        }
        finally {
          unlock()
        }
      }
      else {
        lockFailedValue
      }
  }

  private val files = MachineLearningCompletionModelFiles()

  private val modelLock = ReentrantLock()
  private val appLock = ReentrantLock()
  private fun MachineLearningCompletionRemoteArtifact.getLock() =
    when (this) {
      is MachineLearningCompletionAppArtifact -> appLock
      is MachineLearningCompletionModelArtifact -> modelLock
    }

  val localServerDirectory
    get() = files.localServerDirectory
  val modelVersion
    get() = modelLock.withLock { files.modelVersion }
  val applicationVersion
    get() = appLock.withLock { files.applicationVersion }

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
      files.updateArtifactFromArchive(progress, artifact, zipFile)
    }

  fun tryRunActionOnFiles(action: (MachineLearningCompletionModelFiles) -> Unit): Boolean =
    appLock.withTryLock(false) {
      modelLock.withTryLock(false) {
        if (files.available()) {
          action(files)
          return true
        }
        return false
      }
    }
}
