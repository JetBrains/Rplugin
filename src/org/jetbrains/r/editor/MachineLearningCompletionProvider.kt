/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.google.gson.Gson
import com.intellij.codeInsight.completion.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionServerService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {

  companion object {
    private val serverService = MachineLearningCompletionServerService.getInstance()
    private val executor =
      AppExecutorUtil.createBoundedApplicationPoolExecutor(MachineLearningCompletionProvider.toString(), 1)
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

  private fun processRequest(requestData: MachineLearningCompletionHttpRequest): Future<String> {
    return executor.submit(Callable {
      HttpRequests.post(serverService.serverAddress, "application/json")
        .connect { request ->
          request.write(GSON.toJson(requestData))
          return@connect request.reader.readText()
        }
    })
  }

  private fun processException(exception: Exception) {
    var possiblyWrappedException = exception
    while (possiblyWrappedException is ExecutionException) {
      // Unwrapping
      possiblyWrappedException = possiblyWrappedException.cause as Exception
    }
    LOG.warn("Exception has occurred during the processing of machine learning completion request",
             possiblyWrappedException)
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val startTime = System.currentTimeMillis()
    if (!serverService.shouldAttemptCompletion()) {
      return
    }
    val inputMessage = constructRequest(parameters)
    val futureAnswer = processRequest(inputMessage)

    awaitWithCheckCanceled(futureAnswer)

    val answer = try {
      futureAnswer.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
    } catch (e: Exception) {
      processException(e)
      return
    }

    processAnswerText(answer, result)

    val endTime = System.currentTimeMillis()
    LOG.info("R ML completion took ${endTime - startTime} ms")
  }

  private fun processAnswerText(answer: String, result: CompletionResultSet) {
    var currentCompletionText = ""
    var isEven = true
    for (text in answer.split("\n")) {
      if (isEven) {
        // `text` is actual completion text
        currentCompletionText = text
      } else {
        // `text` is score of completion
        val score = text.toDouble()
        result.addElement(
          PrioritizedLookupElement.withPriority(
            RLookupElement(currentCompletionText, true, AllIcons.Nodes.Favorite, tailText = " $text"),
            score
          )
        )
      }
      isEven = !isEven
    }
  }
}
