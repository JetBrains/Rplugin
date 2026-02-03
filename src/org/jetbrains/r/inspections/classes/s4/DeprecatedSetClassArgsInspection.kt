/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.classes.s4

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.hints.parameterInfo.RArgumentInfo
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.psi.RElementFactory
import com.intellij.r.psi.psi.api.RArgumentList
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.REmptyExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.psi.api.RVisitor
import com.intellij.r.psi.psi.isFunctionFromLibrary
import org.jetbrains.r.inspections.RInspection

class DeprecatedSetClassArgsInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.deprecated.setClass.args.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(call: RCallExpression) {
      if (!call.isFunctionFromLibrary("setClass", "methods")) return
      val argumentInfo = RArgumentInfo.getArgumentInfo(call) ?: return
      findDeprecatedArgument(argumentInfo, "representation", false)
      findDeprecatedArgument(argumentInfo, "access", true)
      findDeprecatedArgument(argumentInfo, "version", true)
      findDeprecatedArgument(argumentInfo, "S3methods", true)
    }

    private fun findDeprecatedArgument(argumentInfo: RArgumentInfo, argumentName: String, isUnused: Boolean) {
      val arg = argumentInfo.getArgumentPassedToParameter(argumentName) ?: return
      val element = if (arg.parent is RNamedArgument) arg.parent else arg
      if (element is REmptyExpression) return

      if (isUnused) {
        myProblemHolder.registerProblem(element.parent,
                                        RBundle.message("inspection.deprecated.setClass.unused.arg.description", argumentName),
                                        ProblemHighlightType.WEAK_WARNING,
                                        element.textRangeInParent,
                                        RemoveDeprecatedArgQuickFix)
      }
      else {
        myProblemHolder.registerProblem(element.parent,
                                        RBundle.message("inspection.deprecated.setClass.representation.description", argumentName),
                                        ProblemHighlightType.WEAK_WARNING,
                                        element.textRangeInParent,
                                        ConvertRepresentationArgQuickFix)
      }
    }
  }

  companion object {
    private object RemoveDeprecatedArgQuickFix : LocalQuickFix {
      override fun getFamilyName() = RBundle.message("inspection.deprecated.setClass.remove.fix.name")

      override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val arg = element.children.first { it.textRangeInParent == descriptor.textRangeInElement }
        removeArgument(arg as RPsiElement)
        CodeStyleManager.getInstance(project).reformat(element)
      }
    }

    private object ConvertRepresentationArgQuickFix : LocalQuickFix {
      override fun getFamilyName() = RBundle.message("inspection.deprecated.setClass.convert.fix.name")

      override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val argList = descriptor.psiElement as RArgumentList
        val reprArg = argList.children.first { it.textRangeInParent == descriptor.textRangeInElement } as RPsiElement
        val repr =
          if (reprArg is RNamedArgument) reprArg.assignedValue ?: return
          else reprArg

        val slots = mutableListOf<RExpression>()
        val contains = mutableListOf<RExpression>()
        when (repr) {
          is RStringLiteralExpression -> contains.add(repr)
          is RCallExpression -> {
            for (arg in repr.argumentList.expressionList) {
              if (arg is RNamedArgument) slots.add(arg)
              else contains.add(arg)
            }
          }
          else -> return
        }

        if (contains.isEmpty() && slots.isEmpty()) {
          removeArgument(reprArg)
          return
        }
        val newArgsText = argList.expressionList.joinToString(", ") { arg ->
          if (arg != reprArg) arg.text
          else buildString {
            if (contains.isNotEmpty()) {
              append("contains = ")
              if (contains.size == 1) append(contains.single().text)
              else append("c(").append(contains.joinToString(", ") { it.text }).append(")")
            }
            if (slots.isNotEmpty()) {
              if (contains.isNotEmpty()) append(", ")
              append("slots = c(").append(slots.joinToString(", ") { it.text }).append(")")
            }
          }
        }

        val newArgs = RElementFactory.createFuncallFromText(project, "tmp($newArgsText)").argumentList
        CodeStyleManager.getInstance(project).reformat(argList.replace(newArgs))
      }
    }

    private fun removeArgument(element: RPsiElement) {
      val commaL = PsiTreeUtil.findSiblingBackward(element, RElementTypes.R_COMMA, null)
      if (commaL != null) element.parent.deleteChildRange(commaL, element)
      else {
        val commaR = PsiTreeUtil.findSiblingForward(element, RElementTypes.R_COMMA, null)
        if (commaR != null) element.parent.deleteChildRange(element, commaR)
        else element.delete()
      }
    }
  }
}

