/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.extractmethod

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.extractMethod.ExtractMethodHelper
import com.intellij.refactoring.extractMethod.SimpleDuplicatesFinder
import com.intellij.refactoring.util.AbstractVariableData
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPrecedenceUtil
import org.jetbrains.r.psi.api.*


object RExtractMethodUtil {
  fun performExtraction(
    project: Project, editor: Editor, file: RFile,
    fragment: RCodeFragment, functionName: String,
    variableData: Array<AbstractVariableData>
  ) {
    val expressions = fragment.expressions
    val parameters = variableData.filter { it.passAsParameter }.map { it.name }
    val parametersToCall = variableData.filter { it.passAsParameter }.map { it.originalName }

    WriteCommandAction.runWriteCommandAction(project) {
      val functionDeclaration = insertFunction(project, file, fragment, functionName, parameters)

      val finder = SimpleDuplicatesFinder(expressions[0], expressions.last(), fragment.outputVariables, variableData)
      val duplicates = ExtractMethodHelper.collectDuplicates(finder, listOf(functionDeclaration.parent), functionDeclaration)
        .filterNot { it.changedOutput == null && fragment.outputVariables.isNotEmpty() }

      val call = replaceWithCall(
        project,
        expressions[0], expressions.last(),
        functionName, fragment.isReturnInstructionInside,
        parametersToCall, fragment.outputVariables.firstOrNull()
      )
      editor.selectionModel.removeSelection()
      editor.caretModel.moveToOffset(call.textRange.startOffset)

      ExtractMethodHelper.replaceDuplicates(call, editor, {
        val match = it.first
        replaceWithCall(
          project,
          match.startElement, match.endElement,
          functionName, fragment.isReturnInstructionInside,
          parametersToCall.map { parameter -> match.changedParameters[parameter]!! }, match.changedOutput
        )
      }, duplicates)
    }
  }

  private fun insertFunction(
    project: Project, file: RFile, fragment: RCodeFragment, functionName: String, parameters: List<String>
  ): RAssignmentStatement {
    val expressions = fragment.expressions
    // TODO: allow extraction not only to top level
    val anchor = PsiTreeUtil.findPrevParent(file, expressions[0])
    val functionText = "$functionName <- function(${parameters.joinToString(", ")}) {\na\n}"
    val functionDeclaration = RElementFactory.createRPsiElementFromText(project, functionText) as RAssignmentStatement
    val function = functionDeclaration.assignedValue as RFunctionExpression

    val newExpressions = if (fragment.outputVariables.isNotEmpty()) {
      val name = fragment.outputVariables.first()
      expressions + listOf(RElementFactory.createRPsiElementFromText(project, "return($name)") as RExpression)
    }
    else {
      expressions
    }

    if (newExpressions.size == 1 && newExpressions[0] !is RLoopStatement && newExpressions[0] !is RIfStatement) {
      function.expression!!.replace(newExpressions[0])
    }
    else {
      val block = function.expression as RBlockExpression
      var previous = block.expressionList[0]
      newExpressions.forEachIndexed { index, expr ->
        if (index == 0) {
          previous = previous.replace(expr) as RExpression
        }
        else {
          previous = block.addAfter(expr, previous) as RExpression
          block.addBefore(RElementFactory.createLeafFromText(project, "\n"), previous)
        }
      }
    }

    val insertedFunctionDeclaration = file.addBefore(functionDeclaration, anchor) as RAssignmentStatement
    file.addBefore(RElementFactory.createLeafFromText(project, "\n"), anchor)
    file.addBefore(RElementFactory.createLeafFromText(project, "\n"), anchor)
    CodeEditUtil.setNodeGeneratedRecursively(insertedFunctionDeclaration.node, true)
    return insertedFunctionDeclaration
  }

  private fun replaceWithCall(
    project: Project, fragmentStart: PsiElement, fragmentEnd: PsiElement, functionName: String,
    isReturn: Boolean, parameters: List<String>, outputVariable: String?
  ): RExpression {
    val callText = "$functionName(${parameters.joinToString(", ") { it }})"
    if (fragmentStart != fragmentEnd) {
      while (true) {
        val stop = fragmentEnd.prevSibling == fragmentStart
        fragmentEnd.prevSibling.delete()
        if (stop) break
      }
    }
    val call = when {
      isReturn -> RElementFactory.createRPsiElementFromText(project, "return($callText)")
      outputVariable != null -> {
        RElementFactory.createRPsiElementFromText(project, "$outputVariable <- $callText")
      }
      else -> RElementFactory.createRPsiElementFromText(project, callText)
    }
    var replacedCall = fragmentEnd.replace(call)
    replacedCall = RPrecedenceUtil.wrapToParenthesisIfNeeded(replacedCall as RExpression, project)
    CodeEditUtil.setNodeGeneratedRecursively(replacedCall.node, true)
    return replacedCall
  }
}
