package org.jetbrains.r.editor

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import org.jetbrains.r.RLanguage

class RMachineLearningCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement().withLanguage(RLanguage.INSTANCE), MachineLearningCompletionProvider())
  }
}