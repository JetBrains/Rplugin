package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.completion.BaseCompletionService
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import org.jetbrains.r.editor.RCompletionContributor
import org.jetbrains.r.editor.RMachineLearningCompletionContributor
import org.jetbrains.r.editor.completion.MachineLearningCompletionLookupDecorator

class MachineLearningCompletionLookupUsageDescriptor : LookupUsageDescriptor {

  private enum class RLookupElementOrigin { ORIGINAL, ML_COMPLETION, MERGED }

  override fun getExtensionKey(): String = "rmlcompletion"

  override fun fillUsageData(lookup: Lookup, usageData: FeatureUsageData) {
    val selectedElement = lookup.currentItem
    if (!lookup.isCompletion || lookup !is LookupImpl
        || selectedElement == null || !selectedElement.isRLookupElement()) {
      return
    }

    val lookupOrigin = selectedElement.`as`(MachineLearningCompletionLookupDecorator.CLASS_CONDITION_KEY)?.origin
                       ?: RLookupElementOrigin.ORIGINAL

    usageData.apply {
      addData("rLookupElementOrigin", lookupOrigin.name)

      MachineLearningCompletionLookupStatistics.get(lookup)?.let { statistics ->
        addData("rMLCompletionEnabled", statistics.mlCompletionIsEnabled)
        addData("rMLCompletionResponseReceived", statistics.mlCompletionResponseReceived)
      }
    }
  }

  private fun LookupElement.isRLookupElement(): Boolean = getUserData(BaseCompletionService.LOOKUP_ELEMENT_CONTRIBUTOR).let { contributor ->
    contributor is RCompletionContributor || contributor is RMachineLearningCompletionContributor
  }

  private val MachineLearningCompletionLookupDecorator.origin: RLookupElementOrigin
    get() = when (this) {
      is MachineLearningCompletionLookupDecorator.New -> RLookupElementOrigin.ML_COMPLETION
      is MachineLearningCompletionLookupDecorator.Merged -> RLookupElementOrigin.MERGED
    }
}
