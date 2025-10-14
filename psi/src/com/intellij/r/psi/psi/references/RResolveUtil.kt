package com.intellij.r.psi.psi.references

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RControlFlowHolder
import com.intellij.r.psi.psi.api.RIdentifierExpression
import java.util.*

object RResolveUtil {
  /**
   * Statically finds the preceding (in term of control flow) accepted by the accessor
   */
  fun findPrecedingInstruction(variable: RIdentifierExpression, accessor: (element: PsiElement) -> PsiElement?): PsiElement? {
    var currentElement: PsiElement = variable
    var rControlFlowHolder = PsiTreeUtil.getParentOfType(currentElement, RControlFlowHolder::class.java)
    while (rControlFlowHolder != null) {
      val variableInstruction = rControlFlowHolder.controlFlow.getInstructionByElement(currentElement)
      if (variableInstruction != null) {
        val queue = LinkedList<Instruction>()
        queue.add(variableInstruction)
        while (queue.isNotEmpty()) {
          val currentInstruction = queue.remove()
          val instructionElement = currentInstruction.element
          if (instructionElement != null) {
            val tableModificator = accessor(instructionElement)
            if (tableModificator != null) {
              return tableModificator
            }
          }
          for (prevInstruction in currentInstruction.allPred()) {
            if (prevInstruction.num() < currentInstruction.num() && prevInstruction !in queue) {
              queue.add(prevInstruction)
            }
          }
        }
      }
      currentElement = rControlFlowHolder
      rControlFlowHolder = PsiTreeUtil.getParentOfType(currentElement, RControlFlowHolder::class.java)
    }
    return null
  }
}