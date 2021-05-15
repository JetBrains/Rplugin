package org.jetbrains.r.classes.s4.classInfo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext
import org.jetbrains.r.classes.s4.RS4Resolver
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.references.RReferenceBase

class RS4ClassReference(literal: RStringLiteralExpression) : RReferenceBase<RStringLiteralExpression>(literal) {
  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> = RS4Resolver.resolveS4ClassName(element).map {
    val element = it.element
    if (element is RStringLiteralExpression) {
      PsiElementResolveResult(RPomTarget.createStringLiteralTarget(element))
    }
    else it
  }.toTypedArray()

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
    RS4ContextProvider.getS4Context(stringLiteral, *RS4ContextProvider.S4_CLASS_USAGE_CONTEXTS) ?: return PsiReference.EMPTY_ARRAY
    return arrayOf(RS4ClassReference(stringLiteral))
  }
}
