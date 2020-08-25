// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.api.RControlFlowHolder
import org.jetbrains.r.psi.api.RParameter

class UnusedParameterInspection : org.jetbrains.r.inspections.RInspection() {

  override fun getDisplayName(): String {
    return RBundle.message("inspection.unusedParameter.name")
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(
    private val myProblemHolder: ProblemsHolder) : org.jetbrains.r.psi.api.RVisitor() {

    override fun visitParameter(o: RParameter) {
      val rControlFlowHolder = PsiTreeUtil.getParentOfType(o, RControlFlowHolder::class.java)
      val variables = rControlFlowHolder?.getLocalVariableInfo(o)?.variables?.get(o.name) ?: return
      if (variables.variableDescription.reads.isEmpty()) {
        myProblemHolder.registerProblem(o, RBundle.message("inspection.message.unused.parameter", o.text), ProblemHighlightType.LIKE_UNUSED_SYMBOL)
      }
    }
  }
}
