// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RControlFlowHolder
import com.intellij.r.psi.psi.api.RInfixOperator
import com.intellij.r.psi.psi.api.ROperator

class ROperatorReference(element: ROperator) : RReferenceBase<ROperator>(element) {

  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    if (element is RInfixOperator) {
        return RResolver.resolveUsingSourcesAndRuntime(element, element.name, resolveLocally())
    }

    val result = ArrayList<ResolveResult>()
    RResolver.resolveInFilesOrLibrary(element, element.text, result)
    return result.toTypedArray()
  }

  private fun resolveLocally(): ResolveResult? {
    val controlFlowHolder = PsiTreeUtil.getParentOfType(element, RControlFlowHolder::class.java)
    val localVariableInfo = controlFlowHolder?.getLocalVariableInfo(element)
    val definition = localVariableInfo?.variables?.get(element.name)?.variableDescription?.firstDefinition
    return if (definition != null && definition.parent is RAssignmentStatement) PsiElementResolveResult(definition.parent) else null
  }

  override fun handleElementRename(newElementName: String): PsiElement? {
    if (element is RInfixOperator) {
      return (element as RInfixOperator).setName(newElementName)
    }
    return null
  }

  override fun getVariants(): Array<Any> {
    return emptyArray()
  }

}
