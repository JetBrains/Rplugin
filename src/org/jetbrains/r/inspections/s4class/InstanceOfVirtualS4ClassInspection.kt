/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.s4class

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.api.RVisitor
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RS4ClassNameIndex

class InstanceOfVirtualS4ClassInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.virtual.s4class.instance.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(call: RCallExpression) {
      if (!call.isFunctionFromLibrary("new", "methods")) return
      val classNameExpression = call.argumentList.expressionList.firstOrNull() as? RStringLiteralExpression ?: return
      val className = classNameExpression.name ?: return
      val infos = RS4ClassNameIndex.findClassInfos(className, call.project, RSearchScopeUtil.getScope(call))
      if (infos.isNotEmpty() && infos.all { it.isVirtual }) {
        myProblemHolder.registerProblem(classNameExpression,
                                        RBundle.message("inspection.virtual.s4class.instance.description", className),
                                        ProblemHighlightType.GENERIC_ERROR)
      }
    }
  }
}

