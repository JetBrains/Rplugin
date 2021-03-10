package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import org.jetbrains.r.settings.MachineLearningCompletionSettings

class MachineLearningCompletionLookupStatistics {

  @Volatile
  var mlCompletionRequestReceived: Boolean = false
    private set
  val mlCompletionIsEnabled: Boolean = MachineLearningCompletionSettings.getInstance().state.isEnabled

  companion object {
    private val KEY =
      Key.create<MachineLearningCompletionLookupStatistics>("MACHINE_LEARNING_COMPLETION_LOOKUP_STATISTICS")

    fun get(lookup: LookupImpl): MachineLearningCompletionLookupStatistics? = lookup.getUserData(KEY)

    fun initStatistics(lookup: LookupImpl) {
      if (get(lookup) == null) {
        lookup.putUserData(KEY, MachineLearningCompletionLookupStatistics())
      }
    }

    fun reportCompletionSuccessfullyFinished(parameters: CompletionParameters) {
      val activeLookup = (parameters.process as? CompletionProgressIndicator)?.lookup
      activeLookup?.let {
        get(it)?.mlCompletionRequestReceived = true
      }
    }
  }
}