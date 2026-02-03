/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4.classInfo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.methods.RS4SetMethodFunctionNameContext
import com.intellij.r.psi.psi.RPomTarget
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.psi.references.RReferenceBase
import com.intellij.r.psi.psi.references.RResolver
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext

class RS4ClassReference(literal: RStringLiteralExpression) : RReferenceBase<RStringLiteralExpression>(literal) {
  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    RS4ContextProvider.getS4Context(psiElement, *RS4ContextProvider.S4_CLASS_USAGE_CONTEXTS) ?: return emptyArray()
    return RResolver.resolveUsingSourcesAndRuntime(element, element.name ?: "", null).map {
      val element = it.element
      if (element is RStringLiteralExpression) {
        PsiElementResolveResult(RPomTarget.createStringLiteralTarget(element))
      }
      else it
    }.toTypedArray()
  }

  override fun getRangeInElement(): TextRange {
    return ElementManipulators.getValueTextRange(element)
  }

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement {
    return element.setName(newElementName)
  }
}

object RS4ClassReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val stringLiteral = element as? RStringLiteralExpression ?: return PsiReference.EMPTY_ARRAY
    RS4ContextProvider.getS4Context(stringLiteral, RS4SetMethodFunctionNameContext::class,
                                    * RS4ContextProvider.S4_CLASS_USAGE_CONTEXTS) ?: return PsiReference.EMPTY_ARRAY
    return arrayOf(RS4ClassReference(stringLiteral))
  }
}
