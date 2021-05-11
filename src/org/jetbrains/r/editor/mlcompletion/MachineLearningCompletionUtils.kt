package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.r.editor.completion.RLookupElement

object MachineLearningCompletionUtils {
  fun LookupElement.isRLookupElement(): Boolean = `as`(RLookupElement::class.java) is RLookupElement

}