/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.rename.NameSuggestionProvider
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.RRefactoringUtil

class RNameSuggestionProvider : NameSuggestionProvider {
  override fun getSuggestedNames(element: PsiElement, nameSuggestionContext: PsiElement?, result: MutableSet<String>): SuggestedNameInfo? {
    if (!(element is RPsiElement || element.containingFile is RFile)) return null
    val elementForRefactoring =
      if (element is PsiNameIdentifierOwner) element.identifyingElement ?: return null
      else element
    val refs = ReferencesSearch.search(elementForRefactoring, LocalSearchScope(RRefactoringUtil.getRScope(element)))
    val shownNames = mutableSetOf<String>()

    val scopes = mutableSetOf(RRefactoringUtil.getRScope(element))
    for (ref in refs) {
      scopes += RRefactoringUtil.getRScope(ref.element)
    }

    scopes.forEach { shownNames += RRefactoringUtil.collectUsedNames(it) }
    shownNames.remove((elementForRefactoring as PsiNamedElement).name)

    result += when {
      element is RParameter -> RNameSuggestion.getVariableSuggestedNames(element.name, shownNames)
      element is RAssignmentStatement && !element.isFunctionDeclaration ->
        RNameSuggestion.getVariableSuggestedNames((element.nameIdentifier as? RIdentifierExpression)?.name, shownNames)
      element is RAssignmentStatement && element.isFunctionDeclaration ->
        RNameSuggestion.getFunctionSuggestedNames((element.nameIdentifier as? RIdentifierExpression)?.name, shownNames)
      element is RIdentifierExpression && element.parent is RForStatement ->
        RNameSuggestion.getTargetForLoopSuggestedNames(element.name, shownNames)
      element is RIdentifierExpression ->
        RNameSuggestion.getVariableSuggestedNames(element.name, shownNames)
      else -> return null
    }

    return null
  }
}