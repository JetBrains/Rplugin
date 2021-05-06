package org.jetbrains.r.codeInsight.findUsages

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SlotDeclarationContext
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RParameter
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RTargetElementEvaluator: TargetElementEvaluatorEx2() {
  override fun isAcceptableNamedParent(parent: PsiElement): Boolean {
    if (parent is RStringLiteralExpression) {
      return RS4ContextProvider.getS4Context(parent, RS4SetClassClassNameContext::class, RS4SlotDeclarationContext::class) != null
    }

    val grandParent = parent.parent
    if (grandParent is RAssignmentStatement) {
      return grandParent.assignee == parent
    }

    return grandParent is RParameter || grandParent is RNamedArgument
  }
}