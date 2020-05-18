/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.cfg

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.*

class VariableDescription(val firstDefinition: RPsiElement) {
  val name = firstDefinition.name!!
  val reads: MutableSet<RPsiElement> = HashSet<RPsiElement>()
  val writes = HashSet<RPsiElement>()
  init {
    writes.add(firstDefinition)
  }
}

data class VariableDefinition(val variableDescription: VariableDescription, val isAlwaysInitialized: Boolean)

data class LocalVariableInfo(val variables: Map<String, VariableDefinition> = HashMap()) {
  fun addVariableDefinition(name: String, variableDefinition: VariableDefinition): LocalVariableInfo {
    val new = HashMap(variables)
    new[name] = variableDefinition
    return LocalVariableInfo(new)
  }
}

data class LocalAnalysisResult(val localVariableInfos: Map<Instruction, LocalVariableInfo>,
                               val closure: Set<VariableDescription>)

private class AnalysisInstance(private val controlFlowHolder: RControlFlowHolder,
                               private val controlFlow2Result: MutableMap<RControlFlowHolder, LocalAnalysisResult>) {
  private val closure = HashSet<VariableDescription>()
  private val controlFlow = controlFlowHolder.controlFlow
  private val result = HashMap<Instruction, LocalVariableInfo>()
  private val variableDescriptions = HashMap<String, VariableDescription>()
  private val innerFunctions = ArrayList<RFunctionExpression>()

  fun runAnalysis(inputState: LocalVariableInfo): LocalAnalysisResult {
    ProgressManager.checkCanceled()
    result[controlFlow.instructions[0]] = inputState
    for (instruction in controlFlow.instructions.drop(1)) {
      val info = join(instruction)
      result[instruction] = transferFunction(instruction, info)
    }
    val exitState = result[controlFlow.instructions.last()]!!
    innerFunctions.forEach { function ->
      val analysisInstance = AnalysisInstance(function, controlFlow2Result)
      val analysisResult = analysisInstance.runAnalysis(exitState)
      for (variableDescriptor in analysisResult.closure) {
        if (variableDescriptor.firstDefinition.getControlFlowContainer() != controlFlowHolder) {
          closure.add(variableDescriptor)
        }
      }
    }
    val analysisResult = LocalAnalysisResult(result, HashSet(closure))
    controlFlow2Result[controlFlowHolder] = analysisResult
    return analysisResult
  }

  private fun transferFunction(instruction: Instruction, info: LocalVariableInfo): LocalVariableInfo {
    ProgressManager.checkCanceled()
    var result: LocalVariableInfo = info
    when (val element = instruction.element) {
      is RAssignmentStatement -> {
        val assignee = element.assignee
        if (assignee is RIdentifierExpression) {
          result = addWrite(info, assignee, element.isClosureAssignment)
        }
      }
      is RParameter -> element.variable?.let {
        result = addWrite(info, it)
      }
      is RIdentifierExpression -> {
        val parent = element.parent
        if (parent !is RParameter && !(parent is RAssignmentStatement && parent.assignee == element)) {
          if (parent is RForStatement && parent.target == element) {
            result = addWrite(info, element)
          }
          else {
            addRead(info, element)
          }
        }
      }
      is RFunctionExpression -> {
        if (controlFlowHolder !is RFile) {
          innerFunctions.add(element)
        }
      }
      is RInfixOperator -> addRead(info, element, element.name)
    }
    return result
  }

  private fun addWrite(info: LocalVariableInfo,
                       element: RIdentifierExpression,
                       isClosureAssignment: Boolean = false): LocalVariableInfo {
    val name = element.name
    val oldVariableDescription = info.variables[name]?.variableDescription
    if (oldVariableDescription == null ||
        oldVariableDescription.firstDefinition.getControlFlowContainer() != controlFlowHolder && !isClosureAssignment) {
      val newVariableDescription = variableDescriptions.getOrPut(name) { VariableDescription(element) }
      return info.addVariableDefinition(name, VariableDefinition(newVariableDescription, true))
    }
    oldVariableDescription.writes.add(element)
    return info
  }

  private fun addRead(info: LocalVariableInfo,
                      element: RIdentifierExpression) {
    addRead(info, element, element.name)
  }

  private fun addRead(info: LocalVariableInfo,
                      element: RPsiElement,
                      name: String) {
    val variableDescription = info.variables[name]?.variableDescription
    if (variableDescription != null) {
      if (variableDescription.firstDefinition.getControlFlowContainer() != controlFlowHolder) {
        closure.add(variableDescription)
      }
      variableDescription.reads.add(element)
    }
  }

  private fun RPsiElement.getControlFlowContainer() = PsiTreeUtil.getParentOfType(this, RControlFlowHolder::class.java)

  private fun join(instruction: Instruction): LocalVariableInfo {
    return join(instruction.allPred()
                  .filter { pred -> controlFlow.isReachable(pred) && pred.num() < instruction.num() }
                  .distinct()
                  .map { result.getValue(it) })
  }

  private fun join(infos: List<LocalVariableInfo>): LocalVariableInfo {
       if (infos.size == 1) {
      return infos.first()
    }
    return LocalVariableInfo(infos.flatMap { it.variables.keys }.map { name ->
      val isAlwaysInitialized = infos.all { it.variables[name]?.isAlwaysInitialized == true}
      val definition = infos.first { it.variables.containsKey(name) }.variables.getValue(name).variableDescription
      name  to VariableDefinition(definition, isAlwaysInitialized)
    }.toMap())
  }
}

fun RControlFlowHolder.analyzeLocals(): Map<RControlFlowHolder, LocalAnalysisResult> {
  val result = HashMap<RControlFlowHolder, LocalAnalysisResult>()
  AnalysisInstance(this, result).runAnalysis(LocalVariableInfo())
  return result
}


