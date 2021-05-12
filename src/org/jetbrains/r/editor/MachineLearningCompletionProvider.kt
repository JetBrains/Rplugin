/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.withTimeout
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService
import com.intellij.util.io.HttpRequests
import org.jetbrains.io.mandatory.NullCheckingFactory
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.editor.mlcompletion.*
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupStatistics
import org.jetbrains.r.editor.mlcompletion.sorting.MachineLearningCompletionSorter
import java.io.IOException
import java.util.concurrent.CompletableFuture

internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {

  companion object {
    private val serverService = MachineLearningCompletionServerService.getInstance()
    private val GSON = GsonBuilder().registerTypeAdapterFactory(NullCheckingFactory.INSTANCE).create()
    private val LOG = Logger.getInstance(MachineLearningCompletionProvider::class.java)
    private val lookupElementFactory = RLookupElementFactory()
  }

  private fun constructRequest(parameters: CompletionParameters): MachineLearningCompletionHttpRequest {
    val previousText = parameters.originalFile.text.subSequence(0, parameters.offset)
    val localTextContent = parameters.position.text
    val isInsideToken = localTextContent != CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
    return MachineLearningCompletionHttpRequest(isInsideToken, previousText)
  }

  private fun processRequest(requestData: MachineLearningCompletionHttpRequest):
    CompletableFuture<MachineLearningCompletionHttpResponse?> {
    return CompletableFuture.supplyAsync(
      {
        try {
          HttpRequests.post(serverService.serverAddress, "application/json")
            .connect { request ->
              request.write(GSON.toJson(requestData))
              return@connect GSON.fromJson(request.reader.readText(),
                                           MachineLearningCompletionHttpResponse::class.java)
            }
        }
        catch (e: IOException) {
          serverService.tryRelaunchServer()
          null
        }
        catch (e: JsonParseException) {
          null
        }
      },
      getAppExecutorService())
  }

  private fun addFutureCompletions(parameters: CompletionParameters,
                                   futureResponse: CompletableFuture<MachineLearningCompletionHttpResponse?>,
                                   result: CompletionResultSet,
                                   startTime: Long): Boolean {
    val sorter = MachineLearningCompletionSorter.createSorter(parameters, result.prefixMatcher)
    result.runRemainingContributors(parameters, result::passResult, true, sorter)

    val timeToWait = startTime - System.currentTimeMillis() + serverService.requestTimeoutMs
    // If request has been received then add completions anyway otherwise wait for result with checkCanceled
    val mlCompletionResponse =
      when {
        futureResponse.isDone -> futureResponse.get()
        else -> withTimeout(timeToWait) {
          awaitWithCheckCanceled(futureResponse)
        }
      } ?: return false

    result.addAllElements(mlCompletionResponse.completionVariants.map(lookupElementFactory::createMachineLearningCompletionLookupElement))
    result.restartCompletionWhenNothingMatches()

    return true
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val startTime = System.currentTimeMillis()
    if (!serverService.shouldAttemptCompletion()) {
      return
    }
    val inputMessage = constructRequest(parameters)
    val futureResponse = processRequest(inputMessage)

    val resultWithSorter = result.withRelevanceSorter(MachineLearningCompletionSorter.createSorter(parameters, result.prefixMatcher))
    val processedResponse = addFutureCompletions(parameters, futureResponse, resultWithSorter, startTime)

    val totalTime = System.currentTimeMillis() - startTime
    LOG.info("R ML completion took ${totalTime} ms")

    val response = futureResponse.takeIf { it.isDone && !it.isCompletedExceptionally }?.get()

    MachineLearningCompletionLookupStatistics.reportCompletionFinished(parameters, response, processedResponse, totalTime)
  }
}
