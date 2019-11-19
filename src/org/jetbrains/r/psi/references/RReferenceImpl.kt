// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.light.LightElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.RElementGenerator
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.parsing.RElementTypes
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

    if (element.getKind() == ReferenceKind.LOCAL_VARIABLE ||
        element.getKind() == ReferenceKind.PARAMETER ||
        element.getKind() == ReferenceKind.CLOSURE) {
      val definition = element.findVariableDefinition()?.variableDescription?.firstDefinition
      if (definition?.parent is RAssignmentStatement || definition?.parent is RParameter) {
        result.add(PsiElementResolveResult(definition.parent))
      }
      else if (definition != null) {
        result.add(PsiElementResolveResult(definition))
      }
      return result.toTypedArray()
    }

    val elementName = element.name
    element.containingFile.runtimeInfo?.let { consoleRuntimeInfo ->
      val variables = consoleRuntimeInfo.rInterop.globalEnvLoader.variables
      variables.firstOrNull { it.name == elementName }?.let {
        return arrayOf(PsiElementResolveResult(RPomTarget.createPsiElementByRValue(it)))
      }
    }

    RResolver.resolveInFileOrLibrary(element, elementName, result)
    return result.toTypedArray()
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
    val oldNameIdentifier = element.node.findChildByType(RElementTypes.R_IDENTIFIER)
    if (oldNameIdentifier != null) {
      val dummyFile = RElementGenerator.createDummyFile(newElementName, false, element.project)
      val identifier = dummyFile.node.firstChildNode.findChildByType(RElementTypes.R_IDENTIFIER)
      if (identifier != null) {
        element.node.replaceChild(oldNameIdentifier, identifier)
      }
    }
    return element
  }


  class RefLookupElement(manager: PsiManager, language: Language, private val refExpression: String) : LightElement(manager, language) {

    fun getRefExpression(): PsiElement {
      return org.jetbrains.r.psi.RElementFactory.createRefExpression(project, refExpression).reference.resolve()!!.parent
    }


    override fun toString(): String = ""
  }


}
