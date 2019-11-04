// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.api.RAssignmentStatement

/**
 * In IJ codebase there seems to be a different inline impls for methods, parameters, consts etc.
 *
 *
 * Best too to complex  example org.intellij.grammar.refactor.BnfInlineRuleActionHandler
 */
class RInlineAssignmentHandler : InlineActionHandler() {

  override fun isEnabledForLanguage(language: Language): Boolean {
    return language == RLanguage.INSTANCE
  }

  override fun canInlineElement(psiElement: PsiElement): Boolean {
    return psiElement is RAssignmentStatement
  }

  override fun inlineElement(project: Project, editor: Editor?, psiElement: PsiElement) {
    // nice example see community: org.jetbrains.plugins.groovy.refactoring.inline.GroovyInlineLocalHandler
    // best example org.intellij.grammar.refactor.BnfInlineRuleActionHandler
    //        CommonRefactoringUtil.showErrorHint(project, editor, "Cool Rule has errors", "Inline Rule", null);

    if (PsiTreeUtil.hasErrorElements(psiElement)) {
      CommonRefactoringUtil.showErrorHint(project, editor, "Rule has errors", "Inline Rule", null)
      return
    }

    val allReferences = ReferencesSearch.search(psiElement).findAll()
    if (allReferences.isEmpty()) {
      CommonRefactoringUtil.showErrorHint(project, editor, "Rule is never used", "Inline Rule", null)
      return
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, psiElement)) return

    // needed to allow for for current caret element only inlining
    var reference: PsiReference? = if (editor != null) TargetElementUtil.findReference(editor, editor.caretModel.offset) else null
    if (reference != null && psiElement != reference.resolve()) {
      reference = null
    }

    val dialog = RInlineAssignmentDialog(project, psiElement as RAssignmentStatement, reference)
    dialog.show()
  }
}
