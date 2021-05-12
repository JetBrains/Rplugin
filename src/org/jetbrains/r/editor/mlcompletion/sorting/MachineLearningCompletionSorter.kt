package org.jetbrains.r.editor.mlcompletion.sorting

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionSorter.defaultSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl
import com.intellij.codeInsight.lookup.Classifier
import com.intellij.codeInsight.lookup.ClassifierFactory
import com.intellij.codeInsight.lookup.LookupElement

object MachineLearningCompletionSorter {

  const val ID = "rMlCompletion"

  fun createSorter(parameters: CompletionParameters, matcher: PrefixMatcher): CompletionSorter {
    val default = defaultSorter(parameters, matcher)
    return (default as? CompletionSorterImpl)
             ?.withClassifier("liftShorter", true, MachineLearningCompletionClassifierFactory)
             ?.withoutClassifiers { it.id == "priority" }
           ?: default
  }

  private object MachineLearningCompletionClassifierFactory : ClassifierFactory<LookupElement>(ID) {
    override fun createClassifier(next: Classifier<LookupElement>): Classifier<LookupElement> =
      MachineLearningCompletionClassifier(next)
  }
}
