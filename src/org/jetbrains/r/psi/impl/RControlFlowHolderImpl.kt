/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.RControlFlowHolder
import org.jetbrains.r.psi.cfg.LocalAnalysisResult
import org.jetbrains.r.psi.cfg.RControlFlow
import org.jetbrains.r.psi.cfg.analyzeLocals
import org.jetbrains.r.psi.cfg.buildControlFlow

abstract class RControlFlowHolderImpl(astNode: ASTNode) : RElementImpl(astNode), RControlFlowHolder {
  override val controlFlow: RControlFlow
    get() {
      return CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result(buildControlFlow(), this) }
    }

  private val analysisResults: Map<RControlFlowHolder, LocalAnalysisResult>
    get() {
      return CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result(buildAnalysisResults(), this) }
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

  @Synchronized
  private fun buildControlFlow() = buildControlFlow(this)

  @Synchronized
  private fun buildAnalysisResults() = analyzeLocals()
}