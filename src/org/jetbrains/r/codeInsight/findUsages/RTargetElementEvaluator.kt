package org.jetbrains.r.codeInsight.findUsages

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.methods.RS4SetGenericFunctionNameContext
import com.intellij.r.psi.classes.s4.context.methods.RS4SetMethodFunctionNameContext
import com.intellij.r.psi.classes.s4.context.setClass.RS4SetClassClassNameContext
import com.intellij.r.psi.classes.s4.context.setClass.RS4SlotDeclarationContext
import com.intellij.r.psi.psi.RPomTarget
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.api.RStringLiteralExpression

class RTargetElementEvaluator : TargetElementEvaluatorEx2() {
  override fun isAcceptableNamedParent(parent: PsiElement): Boolean {
    val grandParent = parent.parent
    if (grandParent is RAssignmentStatement) {
      return grandParent.assignee == parent
    }

    return grandParent is RParameter || grandParent is RNamedArgument
  }

  override fun getNamedElement(element: PsiElement): PsiElement? {
    val parent = element.parent
    if (parent is RStringLiteralExpression) {
      val context = RS4ContextProvider.getS4Context(parent,
                                                    RS4SetClassClassNameContext::class,
                                                    RS4SlotDeclarationContext::class,
                                                    RS4SetGenericFunctionNameContext::class,
                                                    RS4SetMethodFunctionNameContext::class)
      if (context != null) return RPomTarget.createStringLiteralTarget(parent)
    }
    return super.getNamedElement(element)
  }
}