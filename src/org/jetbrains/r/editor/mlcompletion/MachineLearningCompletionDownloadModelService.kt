package org.jetbrains.r.editor.mlcompletion

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    val isBeingDownloaded = AtomicBoolean(false)
  }

  fun checkForUpdatesAndDownloadIfNeeded(project: Project) {
    // Check that project is with RPlugin (or maybe even in listener via separate service)
    if (!newVersionIsAvailable(project)) {
      return
    }
    askForUpdate(project)
  }

  private fun newVersionIsAvailable(project: Project): Boolean {
    return true
  }

  fun downloadModel(project: Project): Boolean {
    if (!isBeingDownloaded.compareAndSet(false, true)) {
      return false
    }
    try {
      download(project)
      return true
    } // Exception processing would be nice, rethrow if progress canceled, notify of something gone wrong
    finally {
      isBeingDownloaded.set(false)
    }
  }

  private fun download(project: Project) {
    val repositoryDescriptor =
      RemoteRepositoryDescription("rcompletion-models", "Models for R Machine Learning Completion",
                                  "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models")
    val modelDescriptor =
      JpsMavenRepositoryLibraryDescriptor("org.jetbrains.r.deps.mlcompletion", "bundled_python_server", "1.0",
                                          "zip", false, emptyList())
    val artifactKinds = setOf(ArtifactKind.ZIP_ARCHIVE)
    JarRepositoryManager.resolveAndDownload(project,
                                            modelDescriptor,
                                            artifactKinds,
                                            Paths.get(System.getProperty("user.home"), "test-maven-download").toString(),
                                            listOf(repositoryDescriptor))
  }

}
