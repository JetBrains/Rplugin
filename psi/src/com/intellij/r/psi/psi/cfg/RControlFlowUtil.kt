/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.cfg

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.r.psi.psi.api.*

object RControlFlowUtil {
  fun getEntryPoint(element: RPsiElement?, controlFlow: RControlFlow): Instruction? {
    if (element == null) return null
    return when (element) {
      is RAssignmentStatement -> getEntryPoint(element.assignedValue, controlFlow)
      is RForStatement -> getEntryPoint(element.range, controlFlow)
      is RFunctionExpression -> controlFlow.getInstructionByElement(element)
      else -> {
        element.children.asSequence().
          filterIsInstance<RExpression>().
          mapNotNull { getEntryPoint(it, controlFlow) }
          .firstOrNull() ?: controlFlow.getInstructionByElement(element)
      }
    }
  }
}