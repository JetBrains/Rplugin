package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
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
      MachineLearningCompletionRemoteArtifact.APP -> appLock
      MachineLearningCompletionRemoteArtifact.MODEL -> modelLock
    }

  val localServerDirectory
    get() = files.localServerDirectory
  val modelVersion
    get() = modelLock.withLock { files.modelVersion }
  val applicationVersion
    get() = appLock.withLock { files.applicationVersion }

  open class UpdateArtifactTask(
    private val artifact: MachineLearningCompletionRemoteArtifact,
    project: Project,
    progressTitle: String
  ): Task.Backgroundable(project, progressTitle, true) {
    override fun run(indicator: ProgressIndicator) = getInstance().updateArtifact(indicator, artifact)
  }

  fun updateArtifact(progress: ProgressIndicator, artifact: MachineLearningCompletionRemoteArtifact) =
    artifact.getLock().withLock<Unit> {
      files.updateArtifactFromArchive(progress, artifact)
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
