/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.google.gson.Gson
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Future


internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {

  companion object {
    private val serverService = MachineLearningCompletionServerService.getInstance()
    private val executor =
      AppExecutorUtil.createBoundedApplicationPoolExecutor(MachineLearningCompletionProvider::class.java.simpleName, 1)
    private val GSON = Gson()
    private val LOG = Logger.getInstance(MachineLearningCompletionProvider::class.java)
    private const val TIMEOUT_MS = 200L
  }

  private fun constructRequest(parameters: CompletionParameters) : MachineLearningCompletionHttpRequest {
    val previousText = parameters.originalFile.text.subSequence(0, parameters.offset)
    val localTextContent = parameters.position.text
    val isInsideToken = localTextContent != CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
    return MachineLearningCompletionHttpRequest(isInsideToken, previousText)
  }

  private fun processRequest(requestData: MachineLearningCompletionHttpRequest):
    Future<MachineLearningCompletionHttpResponse> {
    return executor.submit(Callable {
      try {
        HttpRequests.post(serverService.serverAddress, "application/json")
          .connect { request ->
            request.write(GSON.toJson(requestData))
            return@connect GSON.fromJson(request.reader.readText(), MachineLearningCompletionHttpResponse::class.java)
          }
      } catch (e: IOException) {
        serverService.tryRelaunchServer()
        return@Callable MachineLearningCompletionHttpResponse.emptyResponse
      }
    })
  }

  private fun <T> safeAwaitWithCheckCanceled(future: Future<T>) {
    try {
      awaitWithCheckCanceled(future)
    }
    catch (e: RuntimeException) {
      if (e is ProcessCanceledException) {
        throw e
      }
      LOG.warn("Exception has occurred during machine learning completion request-response processing", e)
    }
  }

  private fun processResponse(response: MachineLearningCompletionHttpResponse, result: CompletionResultSet) {
    result.addAllElements(response.completionVariants.map { it.asLookupElement() })
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val startTime = System.currentTimeMillis()
    if (!serverService.shouldAttemptCompletion()) {
      return
    }
    val inputMessage = constructRequest(parameters)
    val futureResponse = processRequest(inputMessage)

    safeAwaitWithCheckCanceled(futureResponse)
    val response = futureResponse.get()

    processResponse(response, result)

    val endTime = System.currentTimeMillis()
    LOG.info("R ML completion took ${endTime - startTime} ms")
  }
}
