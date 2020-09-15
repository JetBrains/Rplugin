/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.r.editor.completion.RLookupElement
import java.net.Socket

internal class MachineLearningCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val host = "localhost"
  private val port = 7337

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val offset = parameters.offset
    val pre_text = parameters.originalFile.text.subSequence(0, offset)
    var answer = ""
    Socket(host, port).use {
      it.getOutputStream().write((pre_text as String).toByteArray())
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
            RLookupElement(currentCompletionText, true, AllIcons.Nodes.Favorite, tailText = " " + text),
            score
          )
        )
      }
      isEven = !isEven
    }
  }
}