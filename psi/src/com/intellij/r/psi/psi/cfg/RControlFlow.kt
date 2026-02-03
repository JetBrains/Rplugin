/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.cfg

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.codeInsight.controlflow.impl.ControlFlowImpl
import com.intellij.psi.PsiElement
import java.util.BitSet

class RControlFlow(instructions: Array<Instruction>): ControlFlowImpl(instructions) {
  private val reachable = BitSet()
  private val element2Instruction = instructions.mapNotNull { instruction -> instruction.element?.let { it to instruction } }.toMap()

  init {
    ControlFlowUtil.process(this.instructions, 0) { instruction -> reachable.set(instruction.num());true }
  }

  fun getInstructionByElement(element: PsiElement): Instruction? {
    return element2Instruction[element]
  }

  fun isReachable(instruction: Instruction): Boolean {
    if (instruction !== instructions[instruction.num()]) {
      throw IllegalArgumentException("Instruction is not from RControlFlow")
    }
    return reachable.get(instruction.num())
  }
}