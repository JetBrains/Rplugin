package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import org.jetbrains.r.editor.mlcompletion.update.MachineLearningCompletionLocalArtifact
import org.jetbrains.r.editor.mlcompletion.update.VersionConverter
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

    private fun getArtifactVersion(versionFile: String): Version? = File(versionFile).takeIf { it.exists() }
      ?.run { VersionConverter.fromString(readText().trim()) }
  }

  private val modelLock = ReentrantLock()
  private val appLock = ReentrantLock()
  private val MachineLearningCompletionLocalArtifact.lock
    get() = when (this) {
      is MachineLearningCompletionLocalArtifact.Application -> appLock
      is MachineLearningCompletionLocalArtifact.Model -> modelLock
    }

  val localServerDirectory
    get() = MachineLearningCompletionModelFiles.localServerDirectory

  private val _modelVersion = AtomicLazyProperty {
    MachineLearningCompletionModelFiles.modelVersionFilePath?.let { getArtifactVersion(it) }
  }
  val modelVersion
    get() = _modelVersion.get()

  private val _applicationVersion = AtomicLazyProperty {
    MachineLearningCompletionModelFiles.applicationVersionFilePath?.let { getArtifactVersion(it) }
  }
  val applicationVersion
    get() = _applicationVersion.get()

  private val MachineLearningCompletionLocalArtifact.localVersionProperty
    get() = when (this) {
      is MachineLearningCompletionLocalArtifact.Model -> _modelVersion
      is MachineLearningCompletionLocalArtifact.Application -> _applicationVersion
    }

  open class UpdateArtifactTask(
    private val artifact: MachineLearningCompletionLocalArtifact,
    private val artifactZipFile: Path,
    project: Project?,
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

  fun updateArtifact(progress: ProgressIndicator, artifact: MachineLearningCompletionLocalArtifact, zipFile: File) =
    artifact.lock.withLock {
      MachineLearningCompletionModelFiles.updateArtifactFromArchive(progress, artifact, zipFile)
      artifact.localVersionProperty.reset()
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

  fun registerVersionChangeListener(artifact: MachineLearningCompletionLocalArtifact,
                                    disposable: Disposable? = null,
                                    listener: (Version?) -> Unit) {
    val property = artifact.localVersionProperty
    val newVersionSupplier = { listener(property.get()) }

    if (disposable != null) {
      property.afterReset(newVersionSupplier, disposable)
    } else {
      property.afterReset(newVersionSupplier)
    }
  }

  fun validate(artifact: MachineLearningCompletionLocalArtifact) =
    artifact.lock.withLock {
      when (artifact) {
        is MachineLearningCompletionLocalArtifact.Model -> MachineLearningCompletionModelFiles.modelAvailable()
        is MachineLearningCompletionLocalArtifact.Application -> MachineLearningCompletionModelFiles.applicationAvailable()
      }
    }
}
