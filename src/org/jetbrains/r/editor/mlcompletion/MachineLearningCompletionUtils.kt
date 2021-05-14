package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.openapi.util.Key
import org.jetbrains.r.editor.completion.RLookupElement
import org.jetbrains.r.editor.completion.RMachineLearningCompletionLookupElement

object MachineLearningCompletionUtils {
  fun LookupElement.isRLookupElement(): Boolean = `as`(RLookupElement::class.java) is RLookupElement

  fun LookupElement.isRMachineLearningLookupElement(): Boolean =
    `as`(RMachineLearningCompletionLookupElement::class.java) is RMachineLearningCompletionLookupElement

  val LookupElement.priority: Double?
    get() {
      return `as`(PrioritizedLookupElement.CLASS_CONDITION_KEY)?.priority
             ?: `as`(LookupItem.CLASS_CONDITION_KEY)?.priority
    }

  private val mergedKey = Key<Boolean>("IS_MERGED_LOOKUP_ELEMENT")

  fun LookupElement.markAsMergedLookupElement() = putUserData(mergedKey, true)

  fun LookupElement.isMergedLookupElement() = getUserData(mergedKey) ?: false
}
