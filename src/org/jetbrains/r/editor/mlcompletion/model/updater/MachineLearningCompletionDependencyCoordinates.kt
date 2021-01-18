package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.util.SystemInfo
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.version.Version
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService

object MachineLearningCompletionDependencyCoordinates {

  const val REPOSITORY_ID = "rcompletion-models"
  const val REPOSITORY_URL = "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models"
  val REPOSITORY_DESCRIPTOR: RemoteRepository =
    ArtifactRepositoryManager.createRemoteRepository(REPOSITORY_ID, REPOSITORY_URL)

  const val GROUP_ID = "org.jetbrains.r.deps.mlcompletion"

  enum class Artifact(val id: String) {
    MODEL("model"),
    APP(when {
          SystemInfo.isWindows -> "win"
          SystemInfo.isMac -> "macos"
          else -> "linux"
        } + "-app");

    val metadataUrl: String
      get() = listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, "maven-metadata.xml")
        .joinToString("/")

    val currentVersion
      get() = when (this) {
        MODEL -> MachineLearningCompletionModelFilesService.getInstance().modelVersion
        APP -> MachineLearningCompletionModelFilesService.getInstance().applicationVersion
      }

    fun getArtifactUrl(version: String): String =
      listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, version, "$id-$version.zip")
        .joinToString("/")

    lateinit var latestVersion: Version
  }
}
