// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.references

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.r.psi.classes.r6.R6ClassPsiUtil
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.RS4NewObjectSlotNameContext
import com.intellij.r.psi.codeInsight.libraries.RLibrarySupportProvider
import com.intellij.r.psi.codeInsight.table.RTableContextManager
import com.intellij.r.psi.psi.RPomTarget
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.ReferenceKind
import com.intellij.r.psi.psi.TableColumnInfo
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.findVariableDefinition
import com.intellij.r.psi.psi.getKind
import com.intellij.r.psi.psi.getParameters
import com.intellij.r.psi.psi.isDependantIdentifier
import com.intellij.r.psi.skeleton.psi.RSkeletonAssignmentStatement
import com.intellij.util.IncorrectOperationException
import com.intellij.util.Processor

class RReferenceImpl(element: RIdentifierExpression) : RReferenceBase<RIdentifierExpression>(element) {

  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    RPsiUtil.getNamedArgumentByNameIdentifier(element)?.let {
      val argumentResult = resolveNamedArgument(it, incompleteCode)
      if (argumentResult.isNotEmpty()) return argumentResult
    }

    for (extension in RLibrarySupportProvider.EP_NAME.extensions) {
      val resultFromExtension = extension.resolve(element)
      if (resultFromExtension != null) {
        return arrayOf(resultFromExtension)
      }
    }

    if (element.isDependantIdentifier && RS4ContextProvider.getS4Context(element, RS4NewObjectSlotNameContext::class) == null) {
      return resolveDependantIdentifier()
    }

    return RResolver.resolveUsingSourcesAndRuntime(element, element.name, resolveLocally())
  }

  private fun resolveColumn(): PsiElementResolveResult? {
    val resultElementRef = Ref<PsiElement>()
    val resolveProcessor = object : Processor<TableColumnInfo> {
      override fun process(it: TableColumnInfo): Boolean {
        if (it.name == element.name) {
          resultElementRef.set(it.definition)
          return false
        }
        return true
      }
    }

    RTableContextManager.processColumnsInContext(element, resolveProcessor)
    if (!resultElementRef.isNull) {
      return PsiElementResolveResult(resultElementRef.get())
    }
    return null
  }

  private fun resolveLocally(): ResolveResult? {
    val kind = element.getKind()
    if (kind == ReferenceKind.LOCAL_VARIABLE ||
        kind == ReferenceKind.PARAMETER ||
        kind == ReferenceKind.CLOSURE) {
      val definition = element.findVariableDefinition()?.variableDescription?.firstDefinition
      if (definition?.parent is RAssignmentStatement || definition?.parent is RParameter) {
        return PsiElementResolveResult(definition.parent)
      } else if (definition != null) {
        return PsiElementResolveResult(definition)
      }
      return RResolver.EmptyLocalResult
    } else if (kind == ReferenceKind.ARGUMENT) {
      return resolveColumn()
    }
    return null
  }

  private fun resolveNamedArgument(assignment: RNamedArgument,
                                   isIncomplete: Boolean): Array<ResolveResult> {
    val call = assignment.parent?.parent as? RCallExpression ?: return emptyArray()
    return RPsiUtil.resolveCall(call, isIncomplete).mapNotNull { assignment ->
      if (assignment is RSkeletonAssignmentStatement && assignment.parameterNameList.contains(element.name)) {
        return arrayOf(PsiElementResolveResult(RPomTarget.createSkeletonParameterTarget(assignment, element.name)))
      }
      assignment.getParameters().firstOrNull { parameter -> parameter.name == element.name }?.let { PsiElementResolveResult(it) }
    }.toTypedArray()
  }

  private fun resolveDependantIdentifier(): Array<ResolveResult> {
    return R6ClassPsiUtil.getSearchedIdentifier(element)?.let { arrayOf(PsiElementResolveResult(it)) } ?: emptyArray()
  }

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement {
    return element.setName(newElementName)
  }
}
