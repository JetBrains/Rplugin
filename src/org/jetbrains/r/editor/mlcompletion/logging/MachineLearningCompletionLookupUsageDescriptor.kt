package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import org.jetbrains.r.editor.completion.MachineLearningCompletionLookupDecorator
import org.jetbrains.r.editor.completion.RLookupElement

class MachineLearningCompletionLookupUsageDescriptor : LookupUsageDescriptor {

  private enum class RLookupElementOrigin { ORIGINAL, ML_COMPLETION, MERGED }

  override fun getExtensionKey(): String = "r_ml"

  override fun fillUsageData(lookup: Lookup, usageData: FeatureUsageData) {
    val selectedElement = lookup.currentItem
    if (!lookup.isCompletion || lookup !is LookupImpl
        || selectedElement == null || !selectedElement.isRLookupElement()) {
      return
    }

    val lookupOrigin = selectedElement.`as`(MachineLearningCompletionLookupDecorator.CLASS_CONDITION_KEY)?.origin
                       ?: RLookupElementOrigin.ORIGINAL

    usageData.apply {
      addData("r_lookup_element_origin", lookupOrigin.name)

      MachineLearningCompletionLookupStatistics.get(lookup)?.let { statistics ->
        addData("r_ml_enabled", statistics.mlCompletionIsEnabled)
        addData("r_ml_response_received", statistics.mlCompletionResponseReceived)
        addData("r_ml_app_version", statistics.mlCompletionAppVersion)
        addData("r_ml_model_version", statistics.mlCompletionModelVersion)
      }
    }
  }

  private fun LookupElement.isRLookupElement(): Boolean = `as`(RLookupElement::class.java) is RLookupElement

  private val MachineLearningCompletionLookupDecorator.origin: RLookupElementOrigin
    get() = when (this) {
      is MachineLearningCompletionLookupDecorator.New -> RLookupElementOrigin.ML_COMPLETION
      is MachineLearningCompletionLookupDecorator.Merged -> RLookupElementOrigin.MERGED
    }
}
