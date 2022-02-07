package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupResultDescriptor
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.events.EventPair
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isMergedLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRMachineLearningLookupElement
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.contextType
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.mlAppVersion
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.mlEnabled
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.mlModelVersion
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.mlNProposedVariants
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.mlTimeMs
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.lookupElementOrigin
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionCollectorExtension.Companion.responseReceived
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupStatistics.Companion.rStatistics

class MachineLearningCompletionLookupUsageDescriptor : LookupUsageDescriptor {

  internal enum class RLookupElementOrigin { ORIGINAL, ML_COMPLETION, MERGED }

  override fun getExtensionKey(): String = "r_ml"

  override fun getAdditionalUsageData(lookupResultDescriptor: LookupResultDescriptor): List<EventPair<*>> {
    val lookup = lookupResultDescriptor.lookup
    val selectedElement = lookup.currentItem
    if (!lookup.isCompletion || lookup !is LookupImpl
        || selectedElement == null || !selectedElement.isRLookupElement()) {
      return emptyList()
    }
    val data = ArrayList<EventPair<*>>()
    data.add(lookupElementOrigin.with(selectedElement.origin))

    lookup.rStatistics?.let { statistics ->
      data.add(responseReceived.with(statistics.mlCompletionResponseReceived))
      data.add(contextType.with(statistics.completionContextType))
      data.add(mlTimeMs.with(statistics.mlCompletionTimeMs))
      data.add(mlNProposedVariants.with(statistics.mlCompletionNProposedVariants))
      data.add(mlEnabled.with(statistics.mlCompletionIsEnabled))
      data.add(mlAppVersion.with(statistics.mlCompletionAppVersion))
      data.add(mlModelVersion.with(statistics.mlCompletionModelVersion))
    }
    return data
  }

  private val LookupElement.origin: RLookupElementOrigin
    get() = when {
      isMergedLookupElement() -> RLookupElementOrigin.MERGED
      isRMachineLearningLookupElement() -> RLookupElementOrigin.ML_COMPLETION
      else -> RLookupElementOrigin.ORIGINAL
    }
}
