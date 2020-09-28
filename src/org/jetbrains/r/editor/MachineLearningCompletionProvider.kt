/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.net.Socket

internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val settings = MachineLearningCompletionSettings.getInstance()
  private val dummyIdentifier = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED

  private fun constructInputMessage(parameters: CompletionParameters) : ByteArray {
    val previousText = parameters.originalFile.text.subSequence(0, parameters.offset)
    val localTextContent = parameters.position.text

    // if True then we are at the start of the new token
    val newTokenStartIdentifier = if (localTextContent == dummyIdentifier) "0|" else "1|"

    return (newTokenStartIdentifier + previousText).toByteArray()
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    if (!settings.state.isEnabled) {
      return
    }
    val inputMessage = constructInputMessage(parameters)
    var answer: String

    Socket(settings.state.host, settings.state.port).use {
      it.getOutputStream().write(inputMessage)
      answer = it.getInputStream().readAllBytes().toString(Charsets.UTF_8)
    }

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