/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.google.gson.Gson
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.withTimeout
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpResponse
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.io.IOException
import java.util.concurrent.*


internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {

  companion object {
    private val serverService = MachineLearningCompletionServerService.getInstance()
    private val GSON = Gson()
    private val LOG = Logger.getInstance(MachineLearningCompletionProvider::class.java)
  }

  private fun constructRequest(parameters: CompletionParameters): MachineLearningCompletionHttpRequest {
    val previousText = parameters.originalFile.text.subSequence(0, parameters.offset)
    val localTextContent = parameters.position.text
    val isInsideToken = localTextContent != CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
    return MachineLearningCompletionHttpRequest(isInsideToken, previousText)
  }

  private fun processRequest(requestData: MachineLearningCompletionHttpRequest):
    CompletableFuture<MachineLearningCompletionHttpResponse> {
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
          MachineLearningCompletionHttpResponse.emptyResponse
        }
      },
      getAppExecutorService())
  }

  private fun mergePriority(lookupElement: LookupElement,
                            mlVariant: MachineLearningCompletionHttpResponse.CompletionVariant): LookupElement {
    var priority = mlVariant.score
    if (lookupElement is PrioritizedLookupElement<*>) {
      priority = maxOf(lookupElement.priority, priority)
    }
    return PrioritizedLookupElement.withPriority(lookupElement, priority)
  }

  private fun addFutureCompletions(parameters: CompletionParameters,
                                   futureResponse: CompletableFuture<MachineLearningCompletionHttpResponse>,
                                   result: CompletionResultSet,
                                   startTime: Long) {
    val mlCompletionVariantsMap = futureResponse.thenApply { response ->
      response.completionVariants.associateByTo(mutableMapOf()) { it.text }
    }
    val otherCompletionVariants = HashSet<String>()

    result.runRemainingContributors(parameters) {
      val lookupElement = it.lookupElement

      // When running other completion contributors we don't want to wait for the response if it's not ready,
      // except maybe for some small amount of time (e.g. 20 milliseconds total), so we use a non-blocking way to get value of Future
      if (!mlCompletionVariantsMap.isDone) {
        otherCompletionVariants.add(lookupElement.lookupString)
        result.addElement(lookupElement)
        return@runRemainingContributors
      }

      val mlVariant = mlCompletionVariantsMap.get().remove(lookupElement.lookupString)
      if (mlVariant == null) {
        result.addElement(lookupElement)
        return@runRemainingContributors
      }

      val newLookupElement = mergePriority(lookupElement, mlVariant)

      result.addElement(newLookupElement)
    }

    // After all contributors executed we can add our completion whenever we want, so we get value of Future in a blocking way with timeout
    val timeToWait = startTime - System.currentTimeMillis() + serverService.requestTimeoutMs
    // If request has been received then add completions anyway otherwise wait for result with checkCanceled
    val mlCompletionResult =
      when {
        mlCompletionVariantsMap.isDone -> mlCompletionVariantsMap.get()
        else -> withTimeout(timeToWait) {
          awaitWithCheckCanceled(mlCompletionVariantsMap)
        }
      } ?: return

    for (key in otherCompletionVariants) {
      mlCompletionResult.remove(key)
    }
    result.addAllElements(mlCompletionResult.values.map { it.asLookupElement() })
    result.restartCompletionWhenNothingMatches()
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val startTime = System.currentTimeMillis()
    if (!serverService.shouldAttemptCompletion()) {
      return
    }
    val inputMessage = constructRequest(parameters)
    val futureResponse = processRequest(inputMessage)

    addFutureCompletions(parameters, futureResponse, result, startTime)

    val endTime = System.currentTimeMillis()
    LOG.info("R ML completion took ${endTime - startTime} ms")
  }
}
