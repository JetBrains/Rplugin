package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.util.SystemInfo
import org.eclipse.aether.repository.RemoteRepository
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager

object MachineLearningCompletionDependencyCoordinates {

  const val REPOSITORY_ID = "rcompletion-models"
  const val REPOSITORY_URL = "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models"
  val REPOSITORY_DESCRIPTOR: RemoteRepository =
    ArtifactRepositoryManager.createRemoteRepository(REPOSITORY_ID, REPOSITORY_URL)

  const val GROUP_ID = "org.jetbrains.r.deps.mlcompletion"

  const val MODEL_ARTIFACT_ID = "model"
  val APP_ARTIFACT_ID = when {
    SystemInfo.isWindows -> "win"
    SystemInfo.isMac -> "macos"
    SystemInfo.isUnix -> "linux"
    else -> null
  }?.let { "$it-app" }
}
