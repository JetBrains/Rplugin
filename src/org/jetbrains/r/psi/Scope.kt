/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.RControlFlowHolder
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RParameter
import org.jetbrains.r.psi.cfg.VariableDefinition

enum class ReferenceKind {
  LOCAL_VARIABLE,
  CLOSURE,
  PARAMETER,
  OTHER
}

fun RIdentifierExpression.getKind(): ReferenceKind {
  val controlFlowHolder = PsiTreeUtil.getParentOfType(this, RControlFlowHolder::class.java) ?: return ReferenceKind.OTHER
  RPsiUtil.getAssignmentByAssignee(this)?.let {
    return if (it.isClosureAssignment) ReferenceKind.CLOSURE else ReferenceKind.LOCAL_VARIABLE
  }
  findVariableDefinition()?.let { variableDefinition ->
    val firstDefinition = variableDefinition.variableDescription.firstDefinition
    val definitionControlFlowHolder = PsiTreeUtil.getParentOfType(firstDefinition, RControlFlowHolder::class.java)
    if (definitionControlFlowHolder != controlFlowHolder) {
      return ReferenceKind.CLOSURE
    }
    if (firstDefinition.parent is RParameter) {
      return ReferenceKind.PARAMETER
    }
    return ReferenceKind.LOCAL_VARIABLE
  }
  return ReferenceKind.OTHER
}

fun RIdentifierExpression.findVariableDefinition(): VariableDefinition? {
  if (isDependantIdentifier) return null
  val controlFlowHolder = PsiTreeUtil.getParentOfType(this, RControlFlowHolder::class.java) ?: return null
  val localVariableInfo = controlFlowHolder.getLocalVariableInfo(this)
  return localVariableInfo?.variables?.get(name)
}