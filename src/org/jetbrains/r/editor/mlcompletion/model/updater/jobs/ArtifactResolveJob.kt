package org.jetbrains.r.editor.mlcompletion.model.updater.jobs

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.transfer.RepositoryOfflineException
import org.eclipse.aether.transfer.TransferCancelledException
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import java.io.File

class ArtifactResolveJob(
  private val descriptor: JpsMavenRepositoryLibraryDescriptor,
  private val kinds: Set<ArtifactKind>,
  repositories: List<RemoteRepository>,
  localRepositoryPath: File,
  override val progressText: String? = "Downloading artifact",
  override val defaultResult: Collection<Artifact> = emptyList()
) : AetherJob<Collection<Artifact>>(repositories, localRepositoryPath) {

  override fun perform(progress: ProgressIndicator?, manager: ArtifactRepositoryManager): Collection<Artifact> {
    try {
      return manager.resolveDependencyAsArtifact(descriptor.groupId,
                                                 descriptor.artifactId,
                                                 descriptor.version,
                                                 kinds,
                                                 false,
                                                 emptyList())
    }
    catch (e: TransferCancelledException) {
      throw ProcessCanceledException(e)
    }
    catch (e: RepositoryOfflineException) {
      throw e
    }
  }
}
