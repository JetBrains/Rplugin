package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupItem
import org.jetbrains.r.editor.completion.MachineLearningCompletionLookupDecorator
import org.jetbrains.r.editor.completion.RLookupElement

object MachineLearningCompletionUtils {
  fun LookupElement.isRLookupElement(): Boolean = `as`(RLookupElement::class.java) is RLookupElement

  fun LookupElement.isRMachineLearningLookupElement(): Boolean =
    `as`(MachineLearningCompletionLookupDecorator::class.java) is MachineLearningCompletionLookupDecorator

  val LookupElement.priority: Double?
    get() {
      return `as`(PrioritizedLookupElement.CLASS_CONDITION_KEY)?.priority
             ?: `as`(LookupItem.CLASS_CONDITION_KEY)?.priority
    }
}
