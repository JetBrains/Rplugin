package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.lookup.impl.LookupUsageTracker
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsageCollectorExtension

internal class MachineLearningCompletionCollectorExtension : FeatureUsageCollectorExtension {
  override fun getGroupId(): String {
    return LookupUsageTracker.GROUP_ID
  }

  override fun getEventId(): String {
    return LookupUsageTracker.FINISHED_EVENT_ID
  }

  override fun getExtensionFields(): List<EventField<*>> {
    return listOf<EventField<*>>(lookupElementOrigin,
                                 responseReceived,
                                 contextType,
                                 mlTimeMs,
                                 mlNProposedVariants,
                                 mlEnabled,
                                 mlAppVersion,
                                 mlModelVersion)
  }

  companion object {
    val lookupElementOrigin = EventFields.Enum<MachineLearningCompletionLookupUsageDescriptor.RLookupElementOrigin>("r_lookup_element_origin")
    val responseReceived = EventFields.Boolean("r_ml_response_received")
    val contextType = EventFields.Enum<MachineLearningCompletionLookupStatistics.CompletionContextType>("r_context_type")
    val mlTimeMs = EventFields.Int("r_ml_time_ms")
    val mlNProposedVariants = EventFields.Int("r_ml_n_proposed_variants")
    val mlEnabled = EventFields.Boolean("r_ml_enabled")
    val mlAppVersion = EventFields.StringValidatedByRegexp("r_ml_app_version", "version")
    val mlModelVersion = EventFields.StringValidatedByRegexp("r_ml_app_version", "version")
  }
}