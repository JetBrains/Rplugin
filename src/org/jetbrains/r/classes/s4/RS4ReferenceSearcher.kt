/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SingleTargetRequestResultProcessor
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SlotDeclarationContext
import org.jetbrains.r.psi.api.RPsiElement

/**
 * Provides usages search for slots like
 *
 * `setClass('Class', slots = c(s<caret>lot = c('numeric', ext = 'character')))`
 */
class RS4ReferenceSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
  override fun processQuery(p: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
    val elementToSearch = p.elementToSearch as? RPsiElement ?: return
    val context = RS4ContextProvider.getS4Context(elementToSearch, RS4SlotDeclarationContext::class) ?: return
    val searchHelper = PsiSearchHelper.getInstance(p.project)
    val scope = p.effectiveSearchScope
    val singleTargetRequestResultProcessor = SingleTargetRequestResultProcessor(elementToSearch)
    val processor = TextOccurenceProcessor { element, offsetInElement ->
      singleTargetRequestResultProcessor.processTextOccurrence(element, offsetInElement, consumer)
    }
    context.slots.forEach {
      searchHelper.processElementsWithWord(processor, scope, it.name, UsageSearchContext.ANY, true)
    }
  }
}
