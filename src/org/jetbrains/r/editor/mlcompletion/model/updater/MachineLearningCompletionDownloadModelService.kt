package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.ide.IdeBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.nio.file.Path


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    private val LOG = Logger.getInstance(MachineLearningCompletionDownloadModelService::class.java)

    private fun <T> submitBackgroundJob(job: () -> T,
                                        onThrowableCallback: ((Throwable) -> Unit)?,
                                        onSuccessCallback: (T) -> Unit) =
      AppExecutorUtil.getAppExecutorService().execute {
        try {
          onSuccessCallback(job())
        }
        catch (e: Throwable) {
          LOG.info(e)
          onThrowableCallback?.invoke(e)
        }
      }

    private fun <T : Any> submitModalJob(job: () -> T,
                                         title: String = "",
                                         onThrowableCallback: ((Throwable) -> Unit)?,
                                         onSuccessCallback: (T) -> Unit) =
      object : Task.Modal(null, title, true) {

        private lateinit var result: T

        override fun run(indicator: ProgressIndicator) {
          result = job()
        }

        override fun onSuccess() = onSuccessCallback(result)

        override fun onThrowable(error: Throwable) {
          LOG.info(error)
          onThrowableCallback?.invoke(error)
        }
      }.queue()
  }

  data class ArtifactsWithSize(val artifacts: List<MachineLearningCompletionRemoteArtifact>, val size: Long)

  fun initiateUpdateCycle(isModal: Boolean,
                          onThrowableCallback: ((Throwable) -> Unit)? = null,
                          onSuccessCallback: (ArtifactsWithSize) -> Unit) {
    if (isModal) {
      submitModalJob(this::getArtifactsToDownloadWithSize, IdeBundle.message("updates.checking.progress"), onThrowableCallback, onSuccessCallback)
    }
    else {
      submitBackgroundJob(this::getArtifactsToDownloadWithSize, onThrowableCallback, onSuccessCallback)
    }
  }

  fun getArtifactsToDownloadWithSize(): ArtifactsWithSize {
    val artifacts = getArtifactsToDownload()
    val size = getArtifactsSize(artifacts)
    MachineLearningCompletionSettings.getInstance().reportUpdateCheck()
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
    project: Project?,
    title: String
  ) : Task.Backgroundable(project, title, true) {
    override fun run(indicator: ProgressIndicator) =
      HttpRequests.request(artifact.latestArtifactUrl).saveToFile(artifactLocalFile, indicator)
  }

}
