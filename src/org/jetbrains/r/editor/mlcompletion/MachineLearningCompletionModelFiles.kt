package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.ZipUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.intellij.util.io.isFile
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionAppArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionModelArtifact
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionRemoteArtifact
import java.io.File
import java.nio.file.Paths

/**
 * Methods are not thread-safe, do not use directly.
 * Use {@link MachineLearningCompletionModelFilesService} instead.
 */
object MachineLearningCompletionModelFiles {

  private fun resolveWithNullable(first: String?, vararg more: String): String? =
    first?.let {
      return Paths.get(first, *more).toString()
    }

  private fun validateFile(file: String?): Boolean =
    file != null && Paths.get(file).run { exists() && isFile() }

  private fun validateDirectory(directory: String?): Boolean =
    directory != null && Paths.get(directory).run { exists() && isDirectory() }

  private fun File.clearDirectory() {
    if (exists()) {
      FileUtil.delete(toPath())
    }
    mkdir()
  }

  val localServerDirectory = resolveWithNullable(RPluginUtil.helperPathOrNull, "python_server")
    ?.apply { File(this).mkdir() }
  val localServerModelDirectory = resolveWithNullable(localServerDirectory, "model")
  val localServerAppDirectory = resolveWithNullable(localServerDirectory, "app")

  val localServerAppExecutableFile = resolveWithNullable(localServerAppDirectory,
                                                         when {
                                                           SystemInfo.isWindows -> "run_demo.exe"
                                                           else -> "run_demo"
                                                         })
  val localServerConfigFile = resolveWithNullable(localServerAppDirectory, "config.yml")

  val modelVersionFilePath = resolveWithNullable(localServerModelDirectory, "version.txt")
  val applicationVersionFilePath = resolveWithNullable(localServerAppDirectory, "version.txt")

  fun updateArtifactFromArchive(progress: ProgressIndicator, artifact: MachineLearningCompletionRemoteArtifact, zipFile: File) : Boolean {
    val dstDir = File(when (artifact) {
      is MachineLearningCompletionModelArtifact -> localServerModelDirectory
      is MachineLearningCompletionAppArtifact -> localServerAppDirectory
    } ?: return false)

    dstDir.clearDirectory()

    ZipUtil.unzip(progress, dstDir, zipFile, null, null, true)

    return true
  }

  fun available(): Boolean = modelAvailable() && applicationAvailable()

  fun modelAvailable(): Boolean =
    validateDirectory(localServerModelDirectory)

  fun applicationAvailable(): Boolean =
    validateDirectory(localServerAppDirectory) &&
    validateFile(localServerConfigFile) &&
    validateFile(localServerAppExecutableFile)
}
