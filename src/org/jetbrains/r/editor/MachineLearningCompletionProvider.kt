/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.google.gson.Gson
import com.intellij.codeInsight.completion.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled
import com.intellij.psi.impl.search.LowLevelSearchUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionHttpRequest
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.io.IOException
import java.util.concurrent.*


internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val settings = MachineLearningCompletionSettings.getInstance()

  companion object {
    private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("rmlcompletion", 2)
    private val gson = Gson()
    private val LOG = Logger.getInstance(LowLevelSearchUtil::class.java)
    private const val DUMMY_IDENTIFIER = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
    private const val TIMEOUT_MS = 200L
  }

  private fun constructRequest(parameters: CompletionParameters) : MachineLearningCompletionHttpRequest {
    val previousText = parameters.originalFile.text.subSequence(0, parameters.offset)
    val localTextContent = parameters.position.text
    val isInsideToken = localTextContent != DUMMY_IDENTIFIER
    return MachineLearningCompletionHttpRequest(isInsideToken, previousText)
  }

  private fun processRequest(requestData: MachineLearningCompletionHttpRequest): Future<String> {
    return executor.submit(Callable {
      HttpRequests.post("http://${settings.state.host}:${settings.state.port}", "application/json")
        .connect { request ->
          request.write(gson.toJson(requestData))
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
    val message = when (possiblyWrappedException) {
      is IOException -> "IO exception has occurred"
      is TimeoutException -> "Timeout exception"
      is CancellationException -> "Machine learning completion was cancelled"
      is InterruptedException -> "Completion thread has been interrupted"
      else -> "Another kind of exception has occurred"
    }
    LOG.warn(message, possiblyWrappedException)
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val startTime = System.nanoTime()
    if (!settings.state.isEnabled) {
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

    val endTime = System.nanoTime()
    LOG.info("R ML completion took ${endTime - startTime}")
  }

  private fun processAnswerText(answer: String, result: CompletionResultSet) {
    var currentCompletionText = ""
    var isEven = true
    for (text in answer.split("\n")) {
      if (isEven) {
        // `text` is actual completion text
        currentCompletionText = text
      }
      else {
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
