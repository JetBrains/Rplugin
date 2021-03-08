package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.util.io.HttpRequests
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import kotlin.reflect.full.primaryConstructor

interface MachineLearningCompletionArtifact {
  val visibleName: String
  val currentVersion: Version?
  fun localIsMissing(): Boolean
}

sealed class MachineLearningCompletionLocalArtifact : MachineLearningCompletionArtifact {
  override val currentVersion: Version?
    get() = when (this) {
      is Model -> MachineLearningCompletionModelFilesService.getInstance().modelVersion
      is Application -> MachineLearningCompletionModelFilesService.getInstance().applicationVersion
    }

  override fun localIsMissing(): Boolean = !MachineLearningCompletionModelFilesService.getInstance().validate(this)

  object Model : MachineLearningCompletionLocalArtifact() {
    override val visibleName = "model"
  }

  object Application : MachineLearningCompletionLocalArtifact() {
    override val visibleName = "application"
  }
}

sealed class MachineLearningCompletionRemoteArtifact(val localDelegate: MachineLearningCompletionLocalArtifact, val id: String)
  : MachineLearningCompletionArtifact by localDelegate {

  private val latestVersionString: String = getLatestVersionString(id)
  val latestVersion: Version = parseVersionOrThrow(latestVersionString)

  companion object {
    const val REPOSITORY_URL = "https://packages.jetbrains.team/maven/p/mlrcc/r-ml-completion-artifacts"
    const val GROUP_ID = "org.jetbrains.r.deps.mlcompletion"

    fun createSubclassInstances(): List<MachineLearningCompletionRemoteArtifact> =
      MachineLearningCompletionRemoteArtifact::class.sealedSubclasses.mapNotNull { kClass ->
        kClass.primaryConstructor?.call()
      }

    private fun getMetadataUrl(artifactId: String): String =
      listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), artifactId, "maven-metadata.xml")
        .joinToString("/")

    private fun getLatestVersionString(artifactId: String): String {
      val metadataUrl = getMetadataUrl(artifactId)
      val metadata = HttpRequests.request(metadataUrl).readString()
      return MetadataXpp3Reader().read(metadata.byteInputStream()).versioning.latest
    }

    private fun parseVersionOrThrow(versionString: String): Version =
      VersionConverter.fromString(versionString)
      ?: throw IllegalStateException("Encountered version in wrong format while trying to download artifact from maven repository")
  }

  val latestArtifactUrl: String
    get() = listOf(REPOSITORY_URL, *GROUP_ID.split('.').toTypedArray(), id, latestVersionString, "$id-$latestVersionString.zip")
      .joinToString("/")

  fun ignoreLatestVersion() {
    val settings = MachineLearningCompletionSettings.getInstance()
    when (this) {
      is Model -> settings.state.modelLastIgnoredVersion = latestVersion
      is Application -> settings.state.appLastIgnoredVersion = latestVersion
    }
  }

  val ignoredVersion: Version?
    get() {
      val settings = MachineLearningCompletionSettings.getInstance()
      return when (this) {
        is Model -> settings.state.modelLastIgnoredVersion
        is Application -> settings.state.appLastIgnoredVersion
      }
    }

  class Model : MachineLearningCompletionRemoteArtifact(MachineLearningCompletionLocalArtifact.Model, "model")

  class Application : MachineLearningCompletionRemoteArtifact(
    MachineLearningCompletionLocalArtifact.Application,
    when {
      SystemInfo.isWindows -> "win"
      SystemInfo.isMac -> "macos"
      else -> "linux"
    } + "-app")
}
