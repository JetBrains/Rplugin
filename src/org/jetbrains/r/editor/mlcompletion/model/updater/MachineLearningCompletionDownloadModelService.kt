package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SequentialTaskExecutor
import com.intellij.util.io.HttpRequests
import java.nio.file.Path


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    private val LOG = Logger.getInstance(MachineLearningCompletionDownloadModelService::class.java)
    val isBeingDownloaded = AtomicBooleanProperty(false)
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

    private fun <T : Any> submitModalJob(job: () -> T,
                                         project: Project? = null,
                                         title: String = "",
                                         onSuccessCallback: (T) -> Unit) =
      object : Task.Modal(project, title, true) {

        private lateinit var result: T

        override fun run(indicator: ProgressIndicator) {
          result = job()
        }

        override fun onSuccess() = onSuccessCallback(result)

        override fun onThrowable(error: Throwable) = isBeingDownloaded.set(false)

        override fun onCancel() = isBeingDownloaded.set(false)
      }.queue()

    private fun ((ArtifactsWithSize) -> Unit).ignoreEmpty(): (ArtifactsWithSize) -> Unit = { artifactsWithSize ->
      if (artifactsWithSize.artifacts.isNotEmpty()) {
        this(artifactsWithSize)
      }
      else {
        isBeingDownloaded.set(false)
      }
    }
  }

  data class ArtifactsWithSize(val artifacts: List<MachineLearningCompletionRemoteArtifact>, val size: Long)

  fun initiateUpdateCycle(isModal: Boolean,
                          ignoreEmptyResult: Boolean = true,
                          onSuccessCallback: (ArtifactsWithSize) -> Unit) {
    if (!isBeingDownloaded.compareAndSet(false, true)) {
      return
    }

    val callback = if (ignoreEmptyResult) onSuccessCallback.ignoreEmpty() else onSuccessCallback
    if (isModal) {
      // TODO: Move string literal to bundle
      submitModalJob(this::getArtifactsToDownloadWithSize, title = "Checking for updates",
                     onSuccessCallback = callback)
    }
    else {
      submitBackgroundJob(this::getArtifactsToDownloadWithSize, callback)
    }
  }

  fun getArtifactsToDownloadWithSize(): ArtifactsWithSize {
    val artifacts = getArtifactsToDownload()
    val size = getArtifactsSize(artifacts)
    return ArtifactsWithSize(artifacts, size)
  }

  private fun getArtifactsToDownload(): List<MachineLearningCompletionRemoteArtifact> =
    MachineLearningCompletionRemoteArtifact.createSubclassInstances().filter { artifact ->
      val currentVersion = artifact.currentVersion
      val latestVersion = artifact.latestVersion

      currentVersion == null || currentVersion < latestVersion
    }

  private fun getArtifactsSize(artifacts: List<MachineLearningCompletionRemoteArtifact>): Long =
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
