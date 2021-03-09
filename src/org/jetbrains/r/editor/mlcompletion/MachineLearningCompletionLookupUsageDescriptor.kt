package org.jetbrains.r.editor.mlcompletion

import com.intellij.codeInsight.completion.BaseCompletionService
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import org.jetbrains.r.editor.RCompletionContributor
import org.jetbrains.r.editor.RMachineLearningCompletionContributor
import org.jetbrains.r.editor.completion.MachineLearningCompletionLookupDecorator
import org.jetbrains.r.settings.MachineLearningCompletionSettings

class MachineLearningCompletionLookupUsageDescriptor : LookupUsageDescriptor {

  companion object {
    private val settings = MachineLearningCompletionSettings.getInstance()
  }

  private enum class RLookupElementOrigin { ORIGINAL, ML_COMPLETION, MERGED }

  override fun getExtensionKey(): String = "rmlcompletion"

  override fun fillUsageData(lookup: Lookup, usageData: FeatureUsageData) {
    lookup.logIfSelectedRElement { lookupElement ->
      val lookupOrigin = lookupElement.`as`(MachineLearningCompletionLookupDecorator.CLASS_CONDITION_KEY)?.origin
                         ?: RLookupElementOrigin.ORIGINAL

      usageData.apply {
        addData("rLookupElementOrigin", lookupOrigin.name)
        addData("rMLCompletionEnabled", settings.state.isEnabled)
      }
    }
  }

  private inline fun Lookup.logIfSelectedRElement(logger: (LookupElement) -> Unit): Unit? = currentItem?.takeIf { selected ->
    isCompletion
    && this is LookupImpl
    && selected.getUserData(BaseCompletionService.LOOKUP_ELEMENT_CONTRIBUTOR).let { contributor ->
      contributor is RCompletionContributor || contributor is RMachineLearningCompletionContributor
    }
  }?.let(logger)

  private val MachineLearningCompletionLookupDecorator.origin: RLookupElementOrigin
    get() = when (this) {
      is MachineLearningCompletionLookupDecorator.New -> RLookupElementOrigin.ML_COMPLETION
      is MachineLearningCompletionLookupDecorator.Merged -> RLookupElementOrigin.MERGED
    }
}
