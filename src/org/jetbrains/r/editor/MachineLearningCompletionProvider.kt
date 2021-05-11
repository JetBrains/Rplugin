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
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.withTimeout
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.editor.completion.RLookupElementFactory
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRLookupElement
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupStatistics
import java.io.IOException
import java.util.concurrent.CompletableFuture


internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {

  companion object {
    private val serverService = MachineLearningCompletionServerService.getInstance()
    private val GSON = Gson()
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
      },
      getAppExecutorService())
  }

  private fun mergeIfNeeded(result: CompletionResult,
                            mlCompletionVariants: MutableMap<String, MachineLearningCompletionHttpResponse.CompletionVariant>?)
    : CompletionResult {
    val lookupElement = result.lookupElement
    val mlVariant = mlCompletionVariants?.remove(result.lookupElement.lookupString)

    if (mlVariant == null) {
      return result
    }

    val mergedElement = lookupElementFactory.createMergedMachineLearningCompletionLookupElement(lookupElement, mlVariant)
    return result.withLookupElement(mergedElement)
  }

  private fun addFutureCompletions(parameters: CompletionParameters,
                                   futureResponse: CompletableFuture<MachineLearningCompletionHttpResponse?>,
                                   result: CompletionResultSet,
                                   startTime: Long): Boolean {
    val mlCompletionVariantsMap = futureResponse.thenApply { response ->
      response?.completionVariants?.associateByTo(mutableMapOf()) { it.text }
    }
    val unprocessedCompletionResults = HashSet<CompletionResult>()

    result.runRemainingContributors(parameters) {
      val lookupElement = it.lookupElement

      // We don't want to process element coming from contributors unrelated to R language
      // If ml completion response is not ready yet we delay processing, otherwise try to merge and add
      when {
        !lookupElement.isRLookupElement() -> result.passResult(it)
        !mlCompletionVariantsMap.isDone -> unprocessedCompletionResults.add(it)
        else -> result.passResult(mergeIfNeeded(it, mlCompletionVariantsMap.get()))
      }
    }

    val timeToWait = startTime - System.currentTimeMillis() + serverService.requestTimeoutMs
    // If request has been received then add completions anyway otherwise wait for result with checkCanceled
    val mlCompletionResult =
      when {
        mlCompletionVariantsMap.isDone -> mlCompletionVariantsMap.get()
        else -> withTimeout(timeToWait) {
          awaitWithCheckCanceled(mlCompletionVariantsMap)
        }
      }

    if (mlCompletionResult == null) {
      unprocessedCompletionResults.forEach {
        result.passResult(it)
      }
      return false
    }

    unprocessedCompletionResults.forEach {
      result.passResult(mergeIfNeeded(it, mlCompletionResult))
    }

    result.addAllElements(mlCompletionResult.values.map(lookupElementFactory::createMachineLearningCompletionLookupElement))
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

    val processedResponse = addFutureCompletions(parameters, futureResponse, result, startTime)

    val totalTime = System.currentTimeMillis() - startTime
    LOG.info("R ML completion took ${totalTime} ms")

    val response = futureResponse.takeIf { it.isDone && !it.isCompletedExceptionally }?.get()

    MachineLearningCompletionLookupStatistics.reportCompletionFinished(parameters, response, processedResponse, totalTime)
  }
}
