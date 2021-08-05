package org.jetbrains.r.editor.mlcompletion

import com.intellij.lang.LangBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.Decompressor
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.intellij.util.io.isFile
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.editor.mlcompletion.update.MachineLearningCompletionLocalArtifact
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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

  private fun resolvePathToExecutable(localServerAppDirectory: String?): String? =
    resolveWithNullable(localServerAppDirectory, "path_to_executable.txt")
      ?.let { File(it) }
      ?.takeIf { it.exists() }
      ?.let {
        val pathToExecutable = it.readText(Charsets.UTF_8).trim()
        return resolveWithNullable(localServerAppDirectory, pathToExecutable)
      }
    ?: resolveWithNullable(MachineLearningCompletionModelFiles.localServerAppDirectory, "run_demo",
                           when {
                             SystemInfo.isWindows -> "run_demo.exe"
                             else -> "run_demo"
                           })

  val localServerDirectory = resolveWithNullable(RPluginUtil.helperPathOrNull, "python_server")
    ?.apply { File(this).mkdir() }
  val localServerModelDirectory = resolveWithNullable(localServerDirectory, "model")
  val localServerAppDirectory = resolveWithNullable(localServerDirectory, "app")

  val localServerAppExecutableFile = resolvePathToExecutable(localServerAppDirectory)
  val localServerConfigFile = resolveWithNullable(localServerAppDirectory, "config.yml")

  val modelVersionFilePath = resolveWithNullable(localServerModelDirectory, "version.txt")
  val applicationVersionFilePath = resolveWithNullable(localServerAppDirectory, "version.txt")

  fun updateArtifactFromArchive(progress: ProgressIndicator, artifact: MachineLearningCompletionLocalArtifact, zipFile: File) : Boolean {
    val dstDir = File(when (artifact) {
      is MachineLearningCompletionLocalArtifact.Model -> localServerModelDirectory
      is MachineLearningCompletionLocalArtifact.Application -> localServerAppDirectory
    } ?: return false)

    val topLevelDirectoryName: String = findSingleTopLevelDirectory(zipFile) ?: return false

    dstDir.clearDirectory()

    progress.text = LangBundle.message("progress.text.extracting")
    val zip = Decompressor.Zip(zipFile)
      .withZipExtensions()
      .removePrefixPath(topLevelDirectoryName)  // Strip top level directory
    zip.extract(dstDir)

    return true
  }

  // Archive is expected to contain a single top level directory
  // This method verifies it and finds a name of this directory
  private fun findSingleTopLevelDirectory(zipFile: File): String? = ZipFile(zipFile).entries().asSequence()
    .fold<ZipEntry, Path?>(null) { topLevelDirectory, zipEntry ->
      val entryPath = Path.of(zipEntry.name)
      val entryTopLevelDirectory = entryPath.subpath(0, 1)

      if (entryPath == entryTopLevelDirectory && !zipEntry.isDirectory) {
        return@findSingleTopLevelDirectory null
      }

      when (topLevelDirectory) {
        null -> entryTopLevelDirectory
        else -> topLevelDirectory.takeIf { it == entryTopLevelDirectory }
                ?: return@findSingleTopLevelDirectory null
      }
    }?.toString()


  fun available(): Boolean = modelAvailable() && applicationAvailable()

  fun modelAvailable(): Boolean =
    validateDirectory(localServerModelDirectory)

  fun applicationAvailable(): Boolean =
    validateDirectory(localServerAppDirectory) &&
    validateFile(localServerConfigFile) &&
    validateFile(localServerAppExecutableFile)
}
