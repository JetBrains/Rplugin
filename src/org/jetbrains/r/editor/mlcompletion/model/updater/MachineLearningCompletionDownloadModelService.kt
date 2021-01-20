package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SequentialTaskExecutor
import com.intellij.util.io.HttpRequests
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    private val LOG = Logger.getInstance(MachineLearningCompletionDownloadModelService::class.java)
    val isBeingDownloaded = AtomicBoolean(false)
    private val executor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("MachineLearningCompletionUpdateChecker")

    private fun <T> submitBackgroundJob(job: () -> T, onSuccessCallback: (T) -> Unit) =
      executor.execute {
        try {
          onSuccessCallback(job())
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

  private fun getArtifactsToDownload(): List<MachineLearningCompletionRemoteArtifact> =
    MachineLearningCompletionRemoteArtifact.createSubclassInstances().filter { artifact ->
      val currentVersion = artifact.currentVersion
      val latestVersion = artifact.latestVersion

      currentVersion == null || currentVersion < latestVersion
    }

  fun getArtifactsToDownloadDescriptorsAsync(onSuccessCallback: (List<MachineLearningCompletionRemoteArtifact>) -> Unit) {
    if (!isBeingDownloaded.compareAndSet(false, true)) {
      return
    }

    submitBackgroundJob(this::getArtifactsToDownload, onSuccessCallback)
  }

  fun getArtifactsSize(artifacts: List<MachineLearningCompletionRemoteArtifact>): Long =
    artifacts.map { artifact ->
      val artifactUrl = artifact.latestArtifactUrl
      HttpRequests.request(artifactUrl).connect { request ->
        request.connection.contentLengthLong
      }
    }.sum()

  open class DownloadArtifactTask(
    private val artifact: MachineLearningCompletionRemoteArtifact,
    private val artifactLocalFile: Path,
    project: Project,
    title: String
  ) : Task.Backgroundable(project, title, true) {
    override fun run(indicator: ProgressIndicator) =
      HttpRequests.request(artifact.latestArtifactUrl).saveToFile(artifactLocalFile, indicator)
  }

}
