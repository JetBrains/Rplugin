/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RParameter
import org.jetbrains.r.psi.findParenthesisParent
import org.jetbrains.r.refactoring.RIntroduceLocalHandler.Companion.IntroduceOperation

class RIntroduceParameterHandler : RIntroduceLocalHandler() {
  override val dialogTitle = "Introduce parameter"

  override fun isValidIntroduceVariant(element: PsiElement?): Boolean {
    if (!super.isValidIntroduceVariant(element)) return false
    return PsiTreeUtil.getParentOfType(element, RFunctionExpression::class.java)?.parameterList != null
  }

  override fun performReplace(operation: IntroduceOperation): RIdentifierExpression {
    val suggestedName = operation.suggestedNames[0]
    val text = "$suggestedName = expr"
    var declaration = (RElementFactory.createRPsiElementFromText(operation.project, "function($text)") as RFunctionExpression).
      parameterList!!.parameterList[0]
    val replacedOccurrences = mutableListOf<RIdentifierExpression>()
    val function = PsiTreeUtil.getParentOfType(operation.expression, RFunctionExpression::class.java)!!
    val parameterList = function.parameterList ?: error("Function ${function.text} has no parameter list")

    WriteCommandAction.runWriteCommandAction(operation.project) {
      declaration.defaultValue!!.replace(operation.expression)

      for (occurrence in operation.occurrences) {
        replacedOccurrences.add(occurrence.findParenthesisParent().replace(declaration.variable!!) as RIdentifierExpression)
        CodeEditUtil.setNodeGeneratedRecursively(replacedOccurrences.last().node, true)
      }
      operation.editor.selectionModel.removeSelection()
      val anchor = parameterList.lastChild
      if (parameterList.parameterList.isNotEmpty()) {
        parameterList.addBefore(RElementFactory.createLeafFromText(operation.project, ","), anchor)
      }
      declaration = parameterList.addBefore(declaration, anchor) as RParameter
      CodeEditUtil.setNodeGeneratedRecursively(parameterList.node, true)
      operation.editor.selectionModel.removeSelection()
      operation.editor.caretModel.moveToOffset(replacedOccurrences[0].textRange.startOffset)
    }
    operation.replacedOccurrences = replacedOccurrences
    return declaration.variable!!
  }
}
