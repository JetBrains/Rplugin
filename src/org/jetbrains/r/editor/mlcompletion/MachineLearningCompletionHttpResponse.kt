package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.icons.AllIcons
import org.jetbrains.r.editor.completion.RLookupElement
import java.text.DecimalFormat

data class MachineLearningCompletionHttpResponse(val completionVariants: List<CompletionVariant>) {
  data class CompletionVariant(val text: String, val score: Double) {
    fun asLookupElement(): LookupElement =
      PrioritizedLookupElement.withPriority(
        RLookupElement(text, true, AllIcons.Nodes.Favorite, tailText = " ${scoreFormat.format(score)}"),
        score
      )
  }

  companion object {
    private val scoreFormat = DecimalFormat("#.##")
  }
}
