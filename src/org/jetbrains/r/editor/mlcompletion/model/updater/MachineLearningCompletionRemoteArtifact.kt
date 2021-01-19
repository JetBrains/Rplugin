package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.util.SystemInfo
import org.eclipse.aether.version.Version
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService

enum class MachineLearningCompletionRemoteArtifact(val id: String, val visibleName: String) {
  MODEL("model", "model"),
  APP(when {
        SystemInfo.isWindows -> "win"
        SystemInfo.isMac -> "macos"
        else -> "linux"
      } + "-app", "application");

  companion object {
    const val REPOSITORY_URL = "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models"
    const val GROUP_ID = "org.jetbrains.r.deps.mlcompletion"
  }

  val metadataUrl: String
    get() = listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, "maven-metadata.xml")
      .joinToString("/")

  val currentVersion
    get() = when (this) {
      MODEL -> MachineLearningCompletionModelFilesService.getInstance().modelVersion
      APP -> MachineLearningCompletionModelFilesService.getInstance().applicationVersion
    }

  fun getArtifactUrl(): String =
    listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, latestVersion, "$id-$latestVersion.zip")
      .joinToString("/")

  var latestVersion: Version? = null
}
