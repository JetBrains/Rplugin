package org.jetbrains.r.editor.mlcompletion.logging

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Version
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import org.jetbrains.r.editor.mlcompletion.MachineLearningCompletionModelFilesService
import org.jetbrains.r.editor.mlcompletion.update.VersionConverter
import org.jetbrains.r.psi.RElementFilters
import org.jetbrains.r.settings.MachineLearningCompletionSettings

class MachineLearningCompletionLookupStatistics {

  enum class CompletionContextType(val pattern: ElementPattern<in PsiElement>) {

    IDENTIFIER(RElementFilters.IDENTIFIER_FILTER),
    NAMESPACE(RElementFilters.NAMESPACE_REFERENCE_FILTER),
    DOLLAR_ACCESS(RElementFilters.MEMBER_ACCESS_FILTER),
    AT_ACCESS(RElementFilters.AT_ACCESS_FILTER),
    IMPORT(RElementFilters.IMPORT_CONTEXT),
    OPERATOR(RElementFilters.OPERATOR_FILTER),
    // UNKNOWN accepts any PsiElement and thus should always be declared last
    UNKNOWN(PlatformPatterns.psiElement());

    companion object {
      fun getContext(parameters: CompletionParameters): CompletionContextType =
        values().first { it.pattern.accepts(parameters.position) }
    }
  }

  @Volatile
  var mlCompletionResponseReceived: Boolean = false
    private set
  val mlCompletionIsEnabled: Boolean = MachineLearningCompletionSettings.getInstance().state.isEnabled
  val mlCompletionAppVersion: String = getVersionString(filesService.applicationVersion)
  val mlCompletionModelVersion: String = getVersionString(filesService.modelVersion)
  var mlCompletionContextType: CompletionContextType = CompletionContextType.UNKNOWN

  companion object {
    private val filesService = MachineLearningCompletionModelFilesService.getInstance()

    private const val versionDefaultValue = "unknown"

    private val KEY =
      Key.create<MachineLearningCompletionLookupStatistics>("MACHINE_LEARNING_COMPLETION_LOOKUP_STATISTICS")

    private fun getVersionString(version: Version?): String = version?.let(VersionConverter::toString) ?: versionDefaultValue

    val LookupImpl.rStatistics: MachineLearningCompletionLookupStatistics?
      get() = getUserData(KEY)

    fun initStatistics(lookup: LookupImpl) {
      if (lookup.rStatistics == null) {
        lookup.putUserData(KEY, MachineLearningCompletionLookupStatistics())
      }
    }

    fun reportCompletionSuccessfullyFinished(parameters: CompletionParameters) {
      val activeLookup = (parameters.process as? CompletionProgressIndicator)?.lookup
      activeLookup?.rStatistics?.apply {
        mlCompletionContextType = CompletionContextType.getContext(parameters)
        mlCompletionResponseReceived = true
      }
    }
  }
}