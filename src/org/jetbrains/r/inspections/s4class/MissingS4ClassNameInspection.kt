/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.s4class

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.api.RVisitor

class MissingS4ClassNameInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.missing.s4.class.name.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitStringLiteralExpression(str: RStringLiteralExpression) {
      if (str.name != "") return
      RS4ContextProvider.getS4Context(str,
                                      RS4SetClassClassNameContext::class,
                                      *RS4ContextProvider.S4_CLASS_USAGE_CONTEXTS) ?: return
      myProblemHolder.registerProblem(str,
                                      RBundle.message("inspection.missing.s4.class.name.description"),
                                      ProblemHighlightType.GENERIC_ERROR)
    }

  }
}

