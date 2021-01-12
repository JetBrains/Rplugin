package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SequentialTaskExecutor
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.r.RBundle
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.model.updater.jobs.ArtifactResolveJob
import org.jetbrains.r.editor.mlcompletion.model.updater.jobs.CheckUpdateJob
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    private val LOG = Logger.getInstance(MachineLearningCompletionModelFilesService::class.java)
    val isBeingDownloaded = AtomicBoolean(false)
    private val executor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("MachineLearningCompletionUpdateChecker")
    private val completionFilesService = MachineLearningCompletionModelFilesService.getInstance()
  }

  fun getArtifactsToDownloadDescriptorsAsync(onSuccessCallback: (List<JpsMavenRepositoryLibraryDescriptor>) -> Unit) {
    if (!isBeingDownloaded.compareAndSet(false, true)) {
      return
    }

    executor.execute {
      try {
        completionFilesService.useTempDirectory { tmpDir ->
          val startModality = ModalityState.defaultModalityState()
          val result = listOf(MachineLearningCompletionDependencyCoordinates.MODEL_ARTIFACT_ID,
                              MachineLearningCompletionDependencyCoordinates.APP_ARTIFACT_ID).mapNotNull { artifactId ->
            val indicator = EmptyProgressIndicator(startModality)
            val checkUpdateJob = makeCheckUpdateJob(artifactId!!, tmpDir.toFile())
            checkUpdateJob.apply(indicator)
          }
          onSuccessCallback(result)
        }
      }
      catch (ignored: ProcessCanceledException) {
        isBeingDownloaded.set(false)
      }
      catch (e: Throwable) {
        LOG.info(e)
        isBeingDownloaded.set(false)
      }
    }
  }

  private fun makeCheckUpdateJob(artifactId: String, localRepository: File): CheckUpdateJob =
    when (artifactId) {
      MachineLearningCompletionDependencyCoordinates.MODEL_ARTIFACT_ID -> completionFilesService.modelVersion
      MachineLearningCompletionDependencyCoordinates.APP_ARTIFACT_ID -> completionFilesService.applicationVersion
      else -> null
    }.let { version ->
      CheckUpdateJob(version,
                     MachineLearningCompletionDependencyCoordinates.GROUP_ID,
                     artifactId,
                     ArtifactKind.ZIP_ARCHIVE,
                     listOf(MachineLearningCompletionDependencyCoordinates.REPOSITORY_DESCRIPTOR),
                     localRepository)
    }

  fun createDownloadAndUpdateTask(project: Project,
                                  descriptors: Collection<JpsMavenRepositoryLibraryDescriptor>,
                                  onSuccessCallback: () -> Unit,
                                  onFinishedCallback: () -> Unit) =
    object : Task.Backgroundable(project, RBundle.message("rmlcompletion.task.downloadAndUpdate"), true) {
      override fun run(indicator: ProgressIndicator) = descriptors.forEach { desc ->
        completionFilesService.useTempDirectory { tmpDirPath ->
          ArtifactResolveJob(desc,
                             setOf(ArtifactKind.ZIP_ARCHIVE),
                             listOf(MachineLearningCompletionDependencyCoordinates.REPOSITORY_DESCRIPTOR),
                             tmpDirPath.toFile())
            .andThen { completionFilesService.updateArtifacts(it) }
            .apply(indicator)
        }
      }

      override fun onSuccess() = onSuccessCallback()

      override fun onFinished() = onFinishedCallback()
    }
}
