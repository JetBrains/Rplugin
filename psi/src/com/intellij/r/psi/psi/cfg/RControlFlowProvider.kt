/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.cfg

import com.intellij.codeInsight.controlflow.ControlFlowProvider
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RControlFlowHolder

class RControlFlowProvider: ControlFlowProvider {
  override fun getAdditionalInfo(instruction: Instruction): String? = null

  override fun getControlFlow(element: PsiElement) =
    PsiTreeUtil.getParentOfType(element, RControlFlowHolder::class.java, false)?.controlFlow
}