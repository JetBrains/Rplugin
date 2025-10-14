// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.intentions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RLanguage
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Contract


abstract class AbstractRIntention protected constructor() : IntentionAction {
  protected abstract val elementPredicate: PsiElementPredicate

  @Throws(IncorrectOperationException::class)
  protected abstract fun processIntention(element: PsiElement, project: Project, editor: Editor)


  @Throws(IncorrectOperationException::class)
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
    val element = findMatchingElement(psiFile, editor) ?: return
    assert(element.isValid) { element }
    processIntention(element, project, editor)
  }


  private fun findMatchingElement(file: PsiFile, editor: Editor): PsiElement? {
    if (!file.viewProvider.languages.contains(RLanguage.INSTANCE)) {
      return null
    }

    val selectionModel = editor.selectionModel
    if (selectionModel.hasSelection()) {
      val start = selectionModel.selectionStart
      val end = selectionModel.selectionEnd

      if (start in 0..end) {
        val selectionRange = TextRange(start, end)
        var element: PsiElement? = CodeInsightUtilCore.findElementInRange(file, start, end, PsiElement::class.java, RLanguage.INSTANCE)
        while (element != null && element.textRange != null && selectionRange.contains(element.textRange)) {
          if (elementPredicate.satisfiedBy(element)) return element
          element = element.parent
        }
      }
    }

    val position = editor.caretModel.offset
    var element = file.findElementAt(position)
    while (element != null) {
      if (elementPredicate.satisfiedBy(element)) return element
      if (isStopElement(element)) break
      element = element.parent
    }

    element = file.findElementAt(position - 1)
    while (element != null) {
      if (elementPredicate.satisfiedBy(element)) return element
      if (isStopElement(element)) return null
      element = element.parent
    }

    return null
  }

  @Contract(value = "null -> false", pure = true)
  private fun isStopElement(element: PsiElement): Boolean {
    return element is PsiFile
  }

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
    return findMatchingElement(psiFile, editor) != null
  }

  override fun startInWriteAction(): Boolean {
    return true
  }
}
