/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.r.psi.roxygen.RoxygenUtil
import com.intellij.r.psi.roxygen.psi.api.RoxygenParameter

class RoxygenParameterReference(element: RoxygenParameter, textRange: TextRange)
  : RoxygenReferenceBase<RoxygenParameter>(element, textRange) {

  override fun resolve(): PsiElement? {
    return ResolveCache.getInstance(psiElement.project).resolveWithCaching(this, Resolver(), false, false)
  }

  private class Resolver : ResolveCache.AbstractResolver<RoxygenParameterReference, PsiElement?> {
    override fun resolve(reference: RoxygenParameterReference, incompleteCode: Boolean): PsiElement? {
      val psiElement = reference.psiElement
      val function = RoxygenUtil.findAssociatedFunction(psiElement)
      return function?.parameterList?.parameterList?.firstOrNull { psiElement.text == it.name }
    }
  }
}