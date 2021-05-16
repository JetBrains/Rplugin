/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.classes.r6

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.api.RVisitor
import org.jetbrains.r.psi.isFunctionFromLibrary

class UnmatchingR6ClassNameInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.r6class.naming.convention.classname")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(call: RCallExpression) {
      if (!call.isFunctionFromLibrary(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName)) return
      val classNameExpression = call.argumentList.expressionList.firstOrNull() as? RStringLiteralExpression ?: return
      val className = classNameExpression.name ?: return
      val userClassVariableName = R6ClassInfoUtil.getAssociatedClassNameFromR6ClassCall(call) ?: return

      if (className != userClassVariableName) {
        myProblemHolder.registerProblem(classNameExpression,
                                        RBundle.message("inspection.r6class.naming.convention.classname", className),
                                        ProblemHighlightType.GENERIC_ERROR)
      }
    }
  }
}