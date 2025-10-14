/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.RElementFactory
import com.intellij.r.psi.psi.RPrecedenceUtil
import com.intellij.r.psi.psi.api.*
import com.intellij.r.psi.psi.findBlockParent
import com.intellij.r.psi.psi.findParenthesisParent
import org.jetbrains.r.refactoring.RIntroduceLocalHandler.Companion.IntroduceOperation

class RIntroduceVariableHandler : RIntroduceLocalHandler() {
  override val dialogTitle = "Introduce variable"

  override fun isValidIntroduceVariant(element: PsiElement?): Boolean {
    if (!super.isValidIntroduceVariant(element)) return false
    if (PsiTreeUtil.getParentOfType(element, RParameterList::class.java) != null) return false
    return true
  }

  override fun performReplace(operation: IntroduceOperation): RIdentifierExpression {
    val suggestedName = operation.suggestedNames[0]
    var declaration = RElementFactory.createRPsiElementFromText(operation.project, "$suggestedName <- expr") as RAssignmentStatement
    var replacedOccurrences = mutableListOf<RIdentifierExpression>()

    WriteCommandAction.runWriteCommandAction(operation.project) {
      var anchor = findAnchor(operation.occurrences)
      var anchorParent = anchor.parent

      declaration.assignedValue!!.replace(operation.expression)
      RPrecedenceUtil.wrapToParenthesisIfNeeded(declaration.assignedValue!!, operation.project)

      var declarationAdded = false
      var declarationAddedToFirst = false
      for (occurrence in operation.occurrences) {
        if (occurrence == anchor && anchorParent !is RFunctionExpression) {
          if (occurrence == operation.expression) {
            declarationAddedToFirst = true
          }
          declaration = occurrence.replace(declaration) as RAssignmentStatement
          anchor = declaration as RExpression
          declarationAdded = true
          CodeEditUtil.setNodeGeneratedRecursively(declaration.node, true)
        } else {
          replacedOccurrences.add(occurrence.findParenthesisParent().replace(declaration.assignee!!) as RIdentifierExpression)
          if (occurrence == anchor) {
            anchor = replacedOccurrences.last()
          }
          CodeEditUtil.setNodeGeneratedRecursively(replacedOccurrences.last().node, true)
        }
      }
      if (anchorParent !is RFile && anchorParent !is RBlockExpression) {
        anchor = wrapToBraces(anchor, operation)
        anchorParent = anchor.parent
        replacedOccurrences = PsiTreeUtil.findChildrenOfType(anchor, RIdentifierExpression::class.java)
          .filter { it.name == suggestedName }.toMutableList()
        if (declarationAdded) {
          declaration = anchor as RAssignmentStatement
        }
        CodeEditUtil.setNodeGeneratedRecursively(anchorParent.node, true)
      }
      operation.editor.selectionModel.removeSelection()
      if (!declarationAdded) {
        declaration = anchorParent.addBefore(declaration, anchor) as RAssignmentStatement
        CodeEditUtil.setNodeGeneratedRecursively(declaration.node, true)
      }
      if (declarationAddedToFirst) {
        operation.editor.caretModel.moveToOffset(declaration.textRange.startOffset)
      } else {
        operation.editor.caretModel.moveToOffset(replacedOccurrences[0].textRange.startOffset)
      }
    }
    operation.replacedOccurrences = replacedOccurrences
    return declaration.assignee as RIdentifierExpression
  }

  private fun findAnchor(occurrences: List<RExpression>): RExpression {
    val commonParent = PsiTreeUtil.findCommonParent(occurrences)!!
    val blockParent = commonParent.findBlockParent()
    return if (blockParent == commonParent) {
      occurrences
        .map { PsiTreeUtil.findPrevParent(blockParent, it) as RExpression }
        .minByOrNull { it.textRange.startOffset }!!
    } else {
      PsiTreeUtil.findPrevParent(blockParent, commonParent) as RExpression
    }
  }

  private fun wrapToBraces(anchor: RExpression, operation: IntroduceOperation): RExpression {
    val createReturn = anchor.parent is RFunctionExpression && !(anchor is RCallExpression && anchor.expression.name == "return")
    val replacementText = if (createReturn) "{\nreturn(x)\n}" else "{\nx\n}"
    val replacement = RElementFactory.createRPsiElementFromText(operation.project, replacementText) as RBlockExpression
    val placeholder = if (createReturn) {
      (replacement.expressionList[0] as RCallExpression).argumentList.expressionList[0]
    } else {
      replacement.expressionList[0]
    }
    placeholder.replace(anchor)
    val replaced = anchor.replace(replacement) as RBlockExpression
    return replaced.expressionList[0]
  }
}
