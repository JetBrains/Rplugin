package org.jetbrains.r.codeInsight.findUsages

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.methods.RS4SetGenericFunctionNameContext
import org.jetbrains.r.classes.s4.context.methods.RS4SetMethodFunctionNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SlotDeclarationContext
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RParameter
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RTargetElementEvaluator : TargetElementEvaluatorEx2() {
  override fun isAcceptableNamedParent(parent: PsiElement): Boolean {
    val grandParent = parent.parent
    if (grandParent is RAssignmentStatement) {
      return grandParent.assignee == parent
    }

    return when (grandParent) {
      is RParameter, is RNamedArgument -> true
      else -> false
    }
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