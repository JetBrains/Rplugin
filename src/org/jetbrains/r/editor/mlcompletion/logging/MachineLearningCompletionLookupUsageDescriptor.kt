package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.LookupResultDescriptor
import com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor
import com.intellij.codeInsight.lookup.impl.LookupUsageTracker
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsageCollectorExtension
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isMergedLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRLookupElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionUtils.isRMachineLearningLookupElement
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupStatistics.Companion.rStatistics
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.CONTEXT_TYPE
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.LOOKUP_ELEMENT_ORIGIN
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.ML_APP_VERSION
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.ML_ENABLED
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.ML_MODEL_VERSION
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.ML_N_PROPOSED_VARIANTS
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.ML_TIME_MS
import org.jetbrains.r.editor.mlcompletion.logging.MachineLearningCompletionLookupUsageDescriptor.MachineLearningCompletionCollectorExtension.Companion.RESPONSE_RECEIVED

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
    data.add(LOOKUP_ELEMENT_ORIGIN.with(selectedElement.origin))

    lookup.rStatistics?.let { statistics ->
      data.add(RESPONSE_RECEIVED.with(statistics.mlCompletionResponseReceived))
      data.add(CONTEXT_TYPE.with(statistics.completionContextType))
      data.add(ML_TIME_MS.with(statistics.mlCompletionTimeMs))
      data.add(ML_N_PROPOSED_VARIANTS.with(statistics.mlCompletionNProposedVariants))
      data.add(ML_ENABLED.with(statistics.mlCompletionIsEnabled))
      data.add(ML_APP_VERSION.with(statistics.mlCompletionAppVersion))
      data.add(ML_MODEL_VERSION.with(statistics.mlCompletionModelVersion))
    }
    return data
  }

  private val LookupElement.origin: RLookupElementOrigin
    get() = when {
      isMergedLookupElement() -> RLookupElementOrigin.MERGED
      isRMachineLearningLookupElement() -> RLookupElementOrigin.ML_COMPLETION
      else -> RLookupElementOrigin.ORIGINAL
    }

  internal class MachineLearningCompletionCollectorExtension : FeatureUsageCollectorExtension {
    override fun getGroupId(): String {
      return LookupUsageTracker.GROUP_ID
    }

    override fun getEventId(): String {
      return LookupUsageTracker.FINISHED_EVENT_ID
    }

    override fun getExtensionFields(): List<EventField<*>> {
      return listOf<EventField<*>>(LOOKUP_ELEMENT_ORIGIN,
                                   RESPONSE_RECEIVED,
                                   CONTEXT_TYPE,
                                   ML_TIME_MS,
                                   ML_N_PROPOSED_VARIANTS,
                                   ML_ENABLED,
                                   ML_APP_VERSION,
                                   ML_MODEL_VERSION)
    }

    companion object {
      val LOOKUP_ELEMENT_ORIGIN = EventFields.Enum<MachineLearningCompletionLookupUsageDescriptor.RLookupElementOrigin>("r_lookup_element_origin")
      val RESPONSE_RECEIVED = EventFields.Boolean("r_ml_response_received")
      val CONTEXT_TYPE = EventFields.Enum<MachineLearningCompletionLookupStatistics.CompletionContextType>("r_context_type")
      val ML_TIME_MS = EventFields.Int("r_ml_time_ms")
      val ML_N_PROPOSED_VARIANTS = EventFields.Int("r_ml_n_proposed_variants")
      val ML_ENABLED = EventFields.Boolean("r_ml_enabled")
      val ML_APP_VERSION = EventFields.StringValidatedByRegexp("r_ml_app_version", "version")
      val ML_MODEL_VERSION = EventFields.StringValidatedByRegexp("r_ml_app_version", "version")
    }
  }
}
