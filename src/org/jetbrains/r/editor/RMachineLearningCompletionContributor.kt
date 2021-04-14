package org.jetbrains.r.editor

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.RElementFilters


class RMachineLearningCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC,
           psiElement().withLanguage(RLanguage.INSTANCE)
             .andOr(RElementFilters.NAMESPACE_REFERENCE_FILTER,
                    RElementFilters.IDENTIFIER_FILTER,
                    RElementFilters.OPERATOR_FILTER,
                    RElementFilters.MEMBER_ACCESS_FILTER,
                    RElementFilters.AT_ACCESS_FILTER,
                    RElementFilters.IMPORT_CONTEXT),
           MachineLearningCompletionProvider())
  }
}