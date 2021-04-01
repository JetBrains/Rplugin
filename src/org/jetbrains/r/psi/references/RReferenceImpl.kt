// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.elementType
import com.intellij.util.IncorrectOperationException
import com.intellij.util.Processor
import org.jetbrains.r.codeInsight.libraries.RLibrarySupportProvider
import org.jetbrains.r.codeInsight.table.RTableContextManager
import org.jetbrains.r.parsing.RElementTypes.R_MEMBER_EXPRESSION
import org.jetbrains.r.psi.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

class RReferenceImpl(element: RIdentifierExpression) : RReferenceBase<RIdentifierExpression>(element) {

  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    RPsiUtil.getNamedArgumentByNameIdentifier(element)?.let {
      return resolveNamedArgument(it, incompleteCode)
    }

    for (extension in RLibrarySupportProvider.EP_NAME.extensions) {
      val resultFromExtension = extension.resolve(element)
      if (resultFromExtension != null) {
        return arrayOf(resultFromExtension)
      }
    }

    if (element.isDependantIdentifier) {
      return resolveDependantIdentifier(incompleteCode)
    }

    return RResolver.resolveUsingSourcesAndRuntime(element, element.name, resolveLocally())
  }

  private fun resolveColumn() : PsiElementResolveResult? {
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
      }
      else if (definition != null) {
        return PsiElementResolveResult(definition)
      }
      return RResolver.EmptyLocalResult
    }
    else if (kind == ReferenceKind.ARGUMENT) {
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
      assignment.getParameters().firstOrNull { parameter -> parameter.name == element.name}?.let { PsiElementResolveResult(it) }
    }.toTypedArray()
  }

  private fun resolveDependantIdentifier(isIncomplete: Boolean): Array<ResolveResult> {
    val objectDependantIdentifierCall = element.parent

    when (objectDependantIdentifierCall.elementType) {
      R_MEMBER_EXPRESSION -> {
        val objectName = objectDependantIdentifierCall.firstChild
        val classDeclarationStatement = objectName.reference?.resolve()

        print(classDeclarationStatement)
      }
    }

    return emptyArray()
  }

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement? {
    return element.setName(newElementName)
  }
}
