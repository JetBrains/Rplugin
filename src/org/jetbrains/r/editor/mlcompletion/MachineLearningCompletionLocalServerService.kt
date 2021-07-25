package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import java.util.concurrent.CompletableFuture

interface MachineLearningCompletionLocalServerService : Disposable {
  companion object {
    fun getInstance(): MachineLearningCompletionLocalServerService = service()
  }

  val requestTimeoutMs: Int

  fun shouldAttemptCompletion(): Boolean

  fun sendCompletionRequest(requestData: MachineLearningCompletionHttpRequest): CompletableFuture<MachineLearningCompletionHttpResponse?>

  fun prepareForLocalUpdate()
}
