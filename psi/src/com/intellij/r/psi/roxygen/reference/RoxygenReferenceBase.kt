/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.r.psi.roxygen.psi.api.RoxygenPsiElement
import com.intellij.util.IncorrectOperationException

abstract class RoxygenReferenceBase<T>(protected val psiElement: T, textRange: TextRange)
  : PsiReferenceBase<T>(psiElement, textRange) where T : RoxygenPsiElement, T : PsiNamedElement {

  override fun isReferenceTo(element: PsiElement): Boolean {
    val resolve = resolve()
    return if (resolve is PsiNameIdentifierOwner) {
      resolve === element || resolve.identifyingElement === element
    } else {
      resolve === element
    }
  }

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement? {
    return psiElement.setName(newElementName)
  }
}