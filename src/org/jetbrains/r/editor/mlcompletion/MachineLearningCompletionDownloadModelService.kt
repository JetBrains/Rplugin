package org.jetbrains.r.editor.mlcompletion

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
  }

  fun checkForUpdatesAndDownloadIfNeeded(project: Project) {
    if (!newVersionIsAvailable()) {
      return
    }
    downloadModel(project)
  }

  private fun newVersionIsAvailable() : Boolean {
    return true
  }

  private fun downloadModel(project: Project) {
    val repositoryDescriptor =
      RemoteRepositoryDescription("rcompletion-models", "Models for R Machine Learning Completion",
                                  "https://packages.jetbrains.team/maven/p/mlrcc/rcompletion-models")
    val modelDescriptor =
      JpsMavenRepositoryLibraryDescriptor("org.jetbrains.r.deps.mlcompletion", "bundled_python_server", "1.0",
                                          "zip", false, emptyList())
    val artifactKinds = setOf(ArtifactKind.ARTIFACT)
    val roots = JarRepositoryManager.resolveAndDownload(project,
                                                        modelDescriptor,
                                                        artifactKinds,
                                                        System.getProperty("user.home"),
                                                        listOf(repositoryDescriptor))
  }
}
