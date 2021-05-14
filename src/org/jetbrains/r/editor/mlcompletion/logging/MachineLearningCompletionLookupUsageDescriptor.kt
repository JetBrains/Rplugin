package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import org.jetbrains.r.editor.completion.RMachineLearningCompletionLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRMachineLearningLookupElement
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupStatistics.Companion.rStatistics

class MachineLearningCompletionLookupUsageDescriptor : LookupUsageDescriptor {

  private enum class RLookupElementOrigin { ORIGINAL, ML_COMPLETION, MERGED }

  override fun getExtensionKey(): String = "r_ml"

  override fun fillUsageData(lookup: Lookup, usageData: FeatureUsageData) {
    val selectedElement = lookup.currentItem
    if (!lookup.isCompletion || lookup !is LookupImpl
        || selectedElement == null || !selectedElement.isRLookupElement()) {
      return
    }

    val lookupOrigin = selectedElement.`as`(RMachineLearningCompletionLookupElement.CLASS_CONDITION_KEY)?.origin
                       ?: RLookupElementOrigin.ORIGINAL

    usageData.apply {
      addData("r_lookup_element_origin", lookupOrigin.name)

      lookup.rStatistics?.let { statistics ->
        addData("r_ml_response_received", statistics.mlCompletionResponseReceived)
        addData("r_context_type", statistics.completionContextType.name)
        addData("r_ml_time_ms", statistics.mlCompletionTimeMs)
        addData("r_ml_n_proposed_variants", statistics.mlCompletionNProposedVariants)
        addData("r_ml_enabled", statistics.mlCompletionIsEnabled)
        addData("r_ml_app_version", statistics.mlCompletionAppVersion)
        addData("r_ml_model_version", statistics.mlCompletionModelVersion)
      }
    }
  }

  private val RMachineLearningCompletionLookupElement.origin: RLookupElementOrigin
    get() = when {
      isRMachineLearningLookupElement() -> RLookupElementOrigin.ML_COMPLETION
      else -> RLookupElementOrigin.ORIGINAL
    }
}
