package org.jetbrains.r.editor.mlcompletion.sorting

import com.intellij.codeInsight.lookup.Classifier
import com.intellij.codeInsight.lookup.ComparingClassifier
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isMergedLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRMachineLearningLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.markAsMergedLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.priority
import java.util.stream.Collectors
import java.util.stream.StreamSupport

class MachineLearningCompletionClassifier(next: Classifier<LookupElement>)
  : ComparingClassifier<LookupElement>(next, MachineLearningCompletionSorter.ID, true) {

  companion object {
    private const val DEFAULT_PRIORITY = 0.0
  }

  private val mlScores: MutableMap<String, Double> = mutableMapOf()

  override fun getWeight(t: LookupElement, context: ProcessingContext): Comparable<*> =
    mlScores.getOrElse(t.lookupString) {
      t.priority ?: DEFAULT_PRIORITY
    }

  override fun classify(source: MutableIterable<LookupElement>, context: ProcessingContext): MutableIterable<LookupElement> {
    StreamSupport.stream(source.spliterator(), false)
      .collect(Collectors.groupingBy(LookupElement::getLookupString, Collectors.toCollection(::SmartList)))
      .forEach(::resolveCollisions)

    val filtered = source.filterNot { it.isRMachineLearningLookupElement() && it.isMergedLookupElement() }

    return super.classify(filtered, context)
  }

  private fun resolveCollisions(collisions: Map.Entry<String, List<LookupElement>>) {
    val lookupString = collisions.key
    val elements = collisions.value

    require(elements.isNotEmpty())
    if (elements.size == 1) {
      return
    }

    val containsRElements = elements.any { it.isRLookupElement() && !it.isRMachineLearningLookupElement() }
    if (!containsRElements) {
      return
    }

    val mlElement = elements.firstOrNull { it.isRMachineLearningLookupElement() }
                    ?: return
    val mlPriority = mlElement.priority ?: DEFAULT_PRIORITY
    mlScores.putIfAbsent(lookupString, mlPriority)

    elements.forEach { it.markAsMergedLookupElement() }
  }
}
