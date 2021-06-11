package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.ide.IdeBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.annotations.Nls
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.nio.file.Path


class MachineLearningCompletionDownloadModelService {

  companion object {
    fun getInstance() = service<MachineLearningCompletionDownloadModelService>()
    private val LOG = Logger.getInstance(MachineLearningCompletionDownloadModelService::class.java)

    private fun <T> submitBackgroundJob(job: () -> T,
                                        onThrowableCallback: ((Throwable) -> Unit)?,
                                        onSuccessCallback: (T) -> Unit): Unit =
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
                                         @Nls title: String = "",
                                         onThrowableCallback: ((Throwable) -> Unit)?,
                                         onSuccessCallback: (T) -> Unit): Unit =
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
                          reportIgnored: Boolean,
                          onThrowableCallback: ((Throwable) -> Unit)? = null,
                          onSuccessCallback: (ArtifactsWithSize) -> Unit) {
    val job = if (reportIgnored) ::getAllArtifactsToDownloadWithSize else ::getArtifactsToDownloadWithSize
    if (isModal) {
      submitModalJob(job, IdeBundle.message("updates.checking.progress"), onThrowableCallback, onSuccessCallback)
    }
    else {
      submitBackgroundJob(job, onThrowableCallback, onSuccessCallback)
    }
  }

  private fun getAllArtifactsToDownloadWithSize(): ArtifactsWithSize = getArtifactsToDownloadWithSize(true)

  private fun getArtifactsToDownloadWithSize(): ArtifactsWithSize = getArtifactsToDownloadWithSize(false)

  private fun getArtifactsToDownloadWithSize(reportIgnored: Boolean): ArtifactsWithSize {
    val artifacts = getArtifactsToDownload().takeIf { artifacts ->
      reportIgnored
      || artifacts.any { it.latestVersion != it.ignoredVersion }
      || artifacts.any(MachineLearningCompletionRemoteArtifact::localIsMissing)
    } ?: emptyList()
    val size = getArtifactsSize(artifacts)
    MachineLearningCompletionSettings.getInstance().reportUpdateCheck()
    return ArtifactsWithSize(artifacts, size)
  }

  private fun getArtifactsToDownload(): List<MachineLearningCompletionRemoteArtifact> =
    MachineLearningCompletionRemoteArtifact.createSubclassInstances().filter { artifact ->
      val currentVersion = artifact.currentVersion
      val latestVersion = artifact.latestVersion

      currentVersion == null || currentVersion < latestVersion || artifact.localIsMissing()
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
    @Nls title: String
  ) : Task.Backgroundable(project, title, true) {
    override fun run(indicator: ProgressIndicator): Unit =
      HttpRequests.request(artifact.latestArtifactUrl).saveToFile(artifactLocalFile, indicator)
  }

}
