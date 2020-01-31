// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.psi.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement
import java.util.*

class RReferenceImpl(element: RIdentifierExpression) : RReferenceBase<RIdentifierExpression>(element) {

  override fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
    val result = ArrayList<ResolveResult>()
    RPsiUtil.getNamedArgumentByNameIdentifier(element)?.let {
      return resolveNamedArgument(it, incompleteCode)
    }
    if (element.isDependantIdentifier) return emptyArray()

    val localResolveResult = resolveLocally()
    val controlFlowHolder = PsiTreeUtil.getParentOfType(element, RControlFlowHolder::class.java)

    if (controlFlowHolder?.getIncludedSources(element)?.resolveInSources(element, result, localResolveResult?.element) != true) {
      if (localResolveResult != null) {
        if (localResolveResult !is EmptyLocalResult) result.add(localResolveResult)
        if (result.isEmpty()) return emptyArray()
      }
      else {
        RResolver.addSortedResultsInFilesOrLibrary(element, result)
      }
    }

    if (result.isNotEmpty()) {
      val distinct = result.distinct()
      return if (distinct.size > 1) {
        distinct.map { PsiElementResolveResult(it.element!!, false) }.toTypedArray()
      }
      else distinct.toTypedArray()
    }

    val elementName = element.name
    element.containingFile.runtimeInfo?.let { consoleRuntimeInfo ->
      val variables = consoleRuntimeInfo.rInterop.globalEnvLoader.variables
      variables.firstOrNull { it.name == elementName }?.let {
        return arrayOf(PsiElementResolveResult(RPomTarget.createPsiElementByRValue(it)))
      }
    }

    RResolver.resolveInFilesOrLibrary(element, elementName, result)
    return result.toTypedArray()
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
      return EmptyLocalResult
    }
    return null
  }

  private object EmptyLocalResult : ResolveResult {
    override fun getElement(): PsiElement? = null
    override fun isValidResult(): Boolean = false
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

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement? {
    return element.setName(newElementName)
  }
}
