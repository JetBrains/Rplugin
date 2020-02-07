/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RForStatement
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.cfg.RControlFlow
import java.util.*

object RInlineUtil {
  fun getLatestDefs(controlFlow: RControlFlow, varName: String, anchor: PsiElement?): List<PsiElement> {
    if (anchor is RAssignmentStatement) return listOf(anchor)

    val par = anchor?.parent
    if (anchor is RIdentifierExpression && par != null && RPsiUtil.getAssignmentByAssignee(anchor) == par) return listOf(par)

    val instructions = controlFlow.instructions
    var anchorInstr = ControlFlowUtil.findInstructionNumberByElement(instructions, anchor)
    if (anchorInstr < 0) return emptyList()
    if (anchor is RIdentifierExpression) {
      val pred = instructions[anchorInstr].allPred()
      if (!pred.isEmpty()) {
        anchorInstr = pred.iterator().next().num()
      }
    }

    return getLatestDefs(varName, instructions, anchorInstr).mapNotNull { it.element }
  }

  private fun getLatestDefs(varName: String, instructions: Array<Instruction>, anchor: Int): Collection<Instruction> {
    val result = LinkedHashSet<Instruction>()
    ControlFlowUtil.iteratePrev(anchor, instructions) { instruction ->
      val element = instruction.element
      if (element is RAssignmentStatement) {
        if (element.name == varName) {
          result.add(instruction)
          return@iteratePrev ControlFlowUtil.Operation.CONTINUE
        }
      }
      ControlFlowUtil.Operation.NEXT
    }
    return result
  }

  fun getPostRefs(controlFlow: RControlFlow, varName: String, anchor: RAssignmentStatement): List<RIdentifierExpression> {
    val result = mutableListOf<RIdentifierExpression>()
    val instructions = controlFlow.instructions
    val exprInstructionNum = controlFlow.getInstructionByElement(anchor)?.num() ?: return emptyList()

    ControlFlowUtil.iterate(exprInstructionNum, instructions, { instruction: Instruction ->
      val element = instruction.element ?: return@iterate ControlFlowUtil.Operation.NEXT
      if (element !is RIdentifierExpression || element.name != varName || RPsiUtil.isNamedArgument(element)) {
        return@iterate ControlFlowUtil.Operation.NEXT
      }

      // a = 3; for (a in 1:5) {}
      val parent = element.parent
      if (parent is RForStatement && PsiTreeUtil.getChildOfType(parent, RIdentifierExpression::class.java) == element) {
        return@iterate ControlFlowUtil.Operation.CONTINUE
      }

      if (RPsiUtil.getAssignmentByAssignee(element) != null) {
        return@iterate ControlFlowUtil.Operation.CONTINUE
      }
      result.add(element)
      ControlFlowUtil.Operation.NEXT
    }, false)

    return result.toList()
  }

  inline fun showErrorAndExit(project: Project, editor: Editor?, message: String, returnAction: () -> Unit) {
    CommonRefactoringUtil.showErrorHint(project, editor, message, RBundle.message("inline.assignment.handler.error.title"), null)
    returnAction()
  }
}