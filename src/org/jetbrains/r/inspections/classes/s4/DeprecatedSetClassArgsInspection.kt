/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.classes.s4

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.REmptyExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RVisitor
import org.jetbrains.r.psi.isFunctionFromLibrary

class DeprecatedSetClassArgsInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.deprecated.setClass.args.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(call: RCallExpression) {
      if (!call.isFunctionFromLibrary("setClass", "methods")) return
      val argumentInfo = RParameterInfoUtil.getArgumentInfo(call) ?: return
      findDeprecatedArgument(argumentInfo, "representation")
      findDeprecatedArgument(argumentInfo, "access")
      findDeprecatedArgument(argumentInfo, "version")
      findDeprecatedArgument(argumentInfo, "S3methods")
    }

    private fun findDeprecatedArgument(argumentInfo: RArgumentInfo, argumentName: String) {
      val arg = argumentInfo.getArgumentPassedToParameter(argumentName) ?: return
      val element = if (arg.parent is RNamedArgument) arg.parent else arg
      if (element is REmptyExpression) return
      myProblemHolder.registerProblem(element,
                                      RBundle.message("inspection.deprecated.setClass.args.description", argumentName),
                                      ProblemHighlightType.WEAK_WARNING)
    }
  }
}

