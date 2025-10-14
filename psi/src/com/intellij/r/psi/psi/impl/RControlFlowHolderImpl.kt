/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.impl

import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.RElementImpl
import com.intellij.r.psi.psi.api.RControlFlowHolder
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.psi.cfg.LocalAnalysisResult
import com.intellij.r.psi.psi.cfg.RControlFlow
import com.intellij.r.psi.psi.cfg.analyzeLocals
import com.intellij.r.psi.psi.cfg.buildControlFlow
import com.intellij.r.psi.psi.references.IncludedSources
import com.intellij.r.psi.psi.references.analyseIncludedSources

abstract class RControlFlowHolderImpl(astNode: ASTNode) : RElementImpl(astNode), RControlFlowHolder {
  override val controlFlow: RControlFlow
    get() {
      return CachedValuesManager.getCachedValue(this) {
        CachedValueProvider.Result(buildControlFlow(this), this)
      }
    }

  private val analysisResults: Map<RControlFlowHolder, LocalAnalysisResult>
    get() {
      return CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result(analyzeLocals(), this) }
    }

  override val localAnalysisResult: LocalAnalysisResult
  get() {
    var parentFunction = PsiTreeUtil.getParentOfType(this, RFunctionExpression::class.java)
    while (parentFunction != null) {
      val ancestor = PsiTreeUtil.getParentOfType(parentFunction, RFunctionExpression::class.java) ?: break
      parentFunction = ancestor
    }
    if (parentFunction == null) {
      return analysisResults.getValue(this)
    }
    assert(parentFunction is RControlFlowHolderImpl) { "Actual type is ${parentFunction.javaClass}"}
    return (parentFunction as RControlFlowHolderImpl).analysisResults.getValue(this)
  }

  override val includedSources: Map<Instruction, IncludedSources>
    get() = CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result(analyseIncludedSources(), this) }
}