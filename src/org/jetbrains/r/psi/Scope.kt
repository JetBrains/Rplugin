/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.cfg.VariableDefinition

enum class ReferenceKind {
  LOCAL_VARIABLE,
  CLOSURE,
  PARAMETER,
  ARGUMENT,
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

  val elementAncestor = PsiTreeUtil.getParentOfType(this, RArgumentHolder::class.java, RCallExpression::class.java,
                                                    RControlFlowHolder::class.java)
  if (elementAncestor is RArgumentList) {
    return ReferenceKind.ARGUMENT
  }
  return ReferenceKind.OTHER
}

fun RIdentifierExpression.findVariableDefinition(): VariableDefinition? {
  if (isDependantIdentifier) return null
  val controlFlowHolder = PsiTreeUtil.getParentOfType(this, RControlFlowHolder::class.java) ?: return null
  val localVariableInfo = controlFlowHolder.getLocalVariableInfo(this)
  return localVariableInfo?.variables?.get(name)
}