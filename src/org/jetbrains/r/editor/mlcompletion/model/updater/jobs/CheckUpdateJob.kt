package org.jetbrains.r.editor.mlcompletion.model.updater.jobs

import com.intellij.openapi.progress.ProgressIndicator
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.version.Version
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.r.RBundle
import java.io.File

class CheckUpdateJob(currentVersion: Version?,
                     private val groupId: String,
                     private val artifactId: String,
                     private val artifactKind: ArtifactKind,
                     repositories: List<RemoteRepository>,
                     localRepositoryPath: File,
                     override val progressText: String? = RBundle.message("rmlcompletion.job.checkUpdates"),
                     override val defaultResult: JpsMavenRepositoryLibraryDescriptor? = null
) : AetherJob<JpsMavenRepositoryLibraryDescriptor?>(repositories, localRepositoryPath) {

  private val currentVersion = currentVersion ?: ArtifactRepositoryManager.asVersion("0")

  override fun perform(progress: ProgressIndicator?, manager: ArtifactRepositoryManager): JpsMavenRepositoryLibraryDescriptor? {
    val latestVersion = manager.getAvailableVersions(groupId, artifactId, "($currentVersion,)", artifactKind).lastOrNull()
    if (latestVersion == null || currentVersion >= latestVersion) {
      return null
    }
    return JpsMavenRepositoryLibraryDescriptor(groupId,
                                               artifactId,
                                               latestVersion.toString(),
                                               "zip",
                                               false,
                                               emptyList())
  }
}
