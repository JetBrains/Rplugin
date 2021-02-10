package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import icons.RIcons
import org.jetbrains.r.editor.completion.RLookupElement

data class MachineLearningCompletionHttpResponse(val completionVariants: List<CompletionVariant>) {
  data class CompletionVariant(val text: String, val score: Double) {
    fun asLookupElement(): LookupElement =
      PrioritizedLookupElement.withPriority(
        RLookupElement(text, true, RIcons.MachineLearning),
        score
      )
  }

  companion object {
    val emptyResponse = MachineLearningCompletionHttpResponse(emptyList())
  }
}
