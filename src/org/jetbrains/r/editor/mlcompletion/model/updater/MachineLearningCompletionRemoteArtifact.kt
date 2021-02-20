package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.eclipse.aether.util.version.GenericVersionScheme
import org.eclipse.aether.version.Version
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import kotlin.reflect.full.primaryConstructor

sealed class MachineLearningCompletionRemoteArtifact {

  abstract val id: String
  abstract val visibleName: String

  companion object {
    const val REPOSITORY_URL = "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models"
    const val GROUP_ID = "org.jetbrains.r.deps.mlcompletion"

    fun createSubclassInstances() =
      MachineLearningCompletionRemoteArtifact::class.sealedSubclasses.mapNotNull { kClass ->
        try {
          kClass.primaryConstructor?.call()
        }
        catch (e: IllegalArgumentException) {
          null
        }
      }
  }

  val metadataUrl: String
    get() = listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, "maven-metadata.xml")
      .joinToString("/")

  val latestVersion: Version by lazy {
    val artifactMetadataUrl = metadataUrl
    val metadata = HttpRequests.request(artifactMetadataUrl).readString()
    GenericVersionScheme().parseVersion(
      MetadataXpp3Reader().read(metadata.byteInputStream()).versioning.latest
    )
  }

  val currentVersion
    get() = when (this) {
      is MachineLearningCompletionModelArtifact -> MachineLearningCompletionModelFilesService.getInstance().modelVersion
      is MachineLearningCompletionAppArtifact -> MachineLearningCompletionModelFilesService.getInstance().applicationVersion
    }

  val latestArtifactUrl: String
    get() = listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, latestVersion, "$id-$latestVersion.zip")
      .joinToString("/")

  fun ignoreLatestVersion() {
    val settings = MachineLearningCompletionSettings.getInstance()
    when (this) {
      is MachineLearningCompletionModelArtifact -> settings.state.modelLastIgnoredVersion = latestVersion
      is MachineLearningCompletionAppArtifact -> settings.state.appLastIgnoredVersion = latestVersion
    }
  }

  val ignoredVersion: Version?
    get() {
      val settings = MachineLearningCompletionSettings.getInstance()
      return when (this) {
        is MachineLearningCompletionModelArtifact -> settings.state.modelLastIgnoredVersion
        is MachineLearningCompletionAppArtifact -> settings.state.appLastIgnoredVersion
      }
    }
}

class MachineLearningCompletionModelArtifact : MachineLearningCompletionRemoteArtifact() {
  override val id = "model"
  override val visibleName = "model"
}

class MachineLearningCompletionAppArtifact : MachineLearningCompletionRemoteArtifact() {
  override val id = when {
                      SystemInfo.isWindows -> "win"
                      SystemInfo.isMac -> "macos"
                      else -> "linux"
                    } + "-app"
  override val visibleName = "application"
}
