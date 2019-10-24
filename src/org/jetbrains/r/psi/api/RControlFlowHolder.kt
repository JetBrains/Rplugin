/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.api

import com.intellij.codeInsight.controlflow.Instruction
import org.jetbrains.r.psi.cfg.LocalAnalysisResult
import org.jetbrains.r.psi.cfg.LocalVariableInfo
import org.jetbrains.r.psi.cfg.RControlFlow

interface RControlFlowHolder: RPsiElement {
  val controlFlow: RControlFlow
  val localAnalysisResult: LocalAnalysisResult

  fun getLocalVariableInfo(element: RPsiElement): LocalVariableInfo? {
    val instruction = controlFlow.getInstructionByElement(element) ?: return null
    return getLocalVariableInfo(instruction)
  }

  fun getLocalVariableInfo(instruction: Instruction): LocalVariableInfo? {
    return localAnalysisResult.localVariableInfos[instruction]
  }
}