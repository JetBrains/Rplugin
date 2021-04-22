package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Version
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.update.VersionConverter
import org.jetbrains.r.settings.MachineLearningCompletionSettings

class MachineLearningCompletionLookupStatistics {

  @Volatile
  var mlCompletionResponseReceived: Boolean = false
    private set
  val mlCompletionIsEnabled: Boolean = MachineLearningCompletionSettings.getInstance().state.isEnabled
  val mlCompletionAppVersion: String = getVersionString(filesService.applicationVersion)
  val mlCompletionModelVersion: String = getVersionString(filesService.modelVersion)

  companion object {
    private val filesService = MachineLearningCompletionModelFilesService.getInstance()

    private const val versionDefaultValue = "unknown"

    private val KEY =
      Key.create<MachineLearningCompletionLookupStatistics>("MACHINE_LEARNING_COMPLETION_LOOKUP_STATISTICS")

    private fun getVersionString(version: Version?): String = version?.let(VersionConverter::toString) ?: versionDefaultValue

    fun get(lookup: LookupImpl): MachineLearningCompletionLookupStatistics? = lookup.getUserData(KEY)

    fun initStatistics(lookup: LookupImpl) {
      if (get(lookup) == null) {
        lookup.putUserData(KEY, MachineLearningCompletionLookupStatistics())
      }
    }

    fun reportCompletionSuccessfullyFinished(parameters: CompletionParameters) {
      val activeLookup = (parameters.process as? CompletionProgressIndicator)?.lookup
      activeLookup?.let {
        get(it)?.mlCompletionResponseReceived = true
      }
    }
  }
}