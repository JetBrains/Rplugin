package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionNotifications.askForUpdate
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    val isBeingDownloaded = AtomicBoolean(false)

    val rPluginPath = RPluginUtil.helperPathOrNull!!
    val remotes = listOf(
      ArtifactRepositoryManager.createRemoteRepository(MachineLearningCompletionDependencyCoordinates.REPOSITORY_ID,
                                                       MachineLearningCompletionDependencyCoordinates.REPOSITORY_URL)
    )
    val consumer = object : ProgressConsumer {
      override fun consume(message: String?) {
        TODO("Not yet implemented")
      }

      override fun isCanceled(): Boolean {
        return super.isCanceled()
      }
    }
    val repositoryManager = ArtifactRepositoryManager(File(rPluginPath), remotes, ProgressConsumer.DEAF)  // TODO: Replace deaf consumber
  }

  fun checkForUpdatesAndDownloadIfNeeded(project: Project) {
    // Check that project is with RPlugin (or maybe even in listener via separate service)
    if (!newVersionIsAvailable(project)) {
      return
    }
    askForUpdate(project, 10)
  }

  private fun newVersionIsAvailable(project: Project): Boolean {
    val versions = repositoryManager.getAvailableVersions(MachineLearningCompletionDependencyCoordinates.GROUP_ID,
                                                          MachineLearningCompletionDependencyCoordinates.ARTIFACT_ID,
                                                          "[0,)",
                                                          ArtifactKind.ZIP_ARCHIVE)
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
/*    val repositoryDescriptor =
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
                                            listOf(repositoryDescriptor))*/
  }

}
