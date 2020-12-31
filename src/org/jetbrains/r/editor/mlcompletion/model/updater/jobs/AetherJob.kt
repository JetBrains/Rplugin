package org.jetbrains.r.editor.mlcompletion.model.updater.jobs

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.NlsContexts
import org.eclipse.aether.repository.RemoteRepository
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import java.io.File
import java.util.function.Function


abstract class AetherJob<T> internal constructor(private val repositories: List<RemoteRepository>,
                                                 private val localRepositoryPath: File) : Function<ProgressIndicator, T> {
  private val LOG = Logger.getInstance(AetherJob::class.java)
  protected fun canStart(): Boolean {
    return repositories.isNotEmpty()
  }

  override fun apply(indicator: ProgressIndicator): T {
    if (canStart()) {
      indicator.text = progressText
      indicator.isIndeterminate = true
      try {
        return perform(indicator,
                       ArtifactRepositoryManager(localRepositoryPath, repositories, object : ProgressConsumer {
                         override fun consume(message: @NlsContexts.ProgressText String?) {
                           indicator.text = message
                         }

                         override fun isCanceled(): Boolean {
                           return indicator.isCanceled
                         }
                       }))
      }
      catch (e: ProcessCanceledException) {
        throw e
      }
      catch (e: Exception) {
        LOG.info(e)
      }
    }
    return defaultResult
  }

  protected abstract val progressText: @NlsContexts.ProgressText String?

  @Throws(Exception::class)
  protected abstract fun perform(progress: ProgressIndicator?, manager: ArtifactRepositoryManager): T
  protected abstract val defaultResult: T

}