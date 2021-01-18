package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionDependencyCoordinates
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

  private val lock = ReentrantLock()
  val localServerDirectory
    get() = files.localServerDirectory
  val modelVersion
    get() = lock.withLock { files.modelVersion }
  val applicationVersion
    get() = lock.withLock { files.applicationVersion }

  fun updateArtifacts(progress: ProgressIndicator, artifacts: Collection<MachineLearningCompletionDependencyCoordinates.Artifact>) = lock.withLock {
    files.updateArtifacts(progress, artifacts)
  }

  fun tryRunActionOnFiles(action: (MachineLearningCompletionModelFiles) -> Unit): Boolean = lock.withTryLock(false) {
    if (files.available()) {
      action(files)
      return true
    }
    return false
  }

  fun useTempDirectory(synchronousAction: (Path) -> Unit): Boolean {
    val tmpDirectory = files.localServerDirectory?.let { Files.createTempDirectory(Paths.get(it), "tmpDir") }
                       ?: return false
    synchronousAction(tmpDirectory)
    FileUtil.delete(tmpDirectory)
    return true
  }
}
