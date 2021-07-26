package org.jetbrains.r.mock

import com.intellij.util.TimeoutUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionLocalServerService
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class MockMachineLearningCompletionServer : MachineLearningCompletionLocalServerService {

  private class CompletionEventDescriptor(val response: MachineLearningCompletionHttpResponse?, val delayMs: Int)

  private val onNext: AtomicReference<CompletionEventDescriptor?> = AtomicReference(null)

  override fun shouldAttemptCompletion(): Boolean = true

  fun returnOnNextCompletion(response: MachineLearningCompletionHttpResponse, delayMs: Int = 0): Unit =
    onNext.set(CompletionEventDescriptor(response, delayMs))

  override fun sendCompletionRequest(requestData: MachineLearningCompletionHttpRequest): CompletableFuture<MachineLearningCompletionHttpResponse?> {
    val descriptor = onNext.getAndSet(null)
                     ?: return CompletableFuture.completedFuture(null)

    return when {
      descriptor.delayMs <= 0 -> CompletableFuture.completedFuture(descriptor.response)
      descriptor.delayMs > MachineLearningCompletionSettings.DEFAULT_REQUEST_TIMEOUT_MS -> CompletableFuture() // Never completes
      else -> CompletableFuture.supplyAsync(
        {
          TimeoutUtil.sleep(descriptor.delayMs.toLong())
          descriptor.response
        }, AppExecutorUtil.getAppExecutorService())
    }
  }

  override fun prepareForLocalUpdate() {
  }

  override fun dispose() {
  }
}