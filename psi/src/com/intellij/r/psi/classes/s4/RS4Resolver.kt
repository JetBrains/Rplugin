/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassInfoUtil
import com.intellij.r.psi.classes.s4.classInfo.RS4DistanceToANY
import com.intellij.r.psi.classes.s4.classInfo.RS4InfDistance
import com.intellij.r.psi.classes.s4.classInfo.RS4OrdDistance
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.RS4NewObjectSlotNameContext
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.associatedS4GenericInfo
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.associatedS4MethodInfo
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.methodName
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.methodNameIdentifier
import com.intellij.r.psi.classes.s4.methods.RS4MethodsUtil.toS4MethodParameters
import com.intellij.r.psi.console.runtimeInfo
import com.intellij.r.psi.hints.parameterInfo.RArgumentInfo
import com.intellij.r.psi.psi.RPomTarget
import com.intellij.r.psi.psi.api.RAtExpression
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.psi.references.RSearchScopeUtil
import com.intellij.r.psi.psi.stubs.classes.RS4ClassNameIndex
import com.intellij.r.psi.psi.stubs.classes.RS4GenericIndex
import com.intellij.r.psi.psi.stubs.classes.RS4MethodsIndex
import com.intellij.r.psi.skeleton.psi.RSkeletonCallExpression

object RS4Resolver {

  fun resolveSlot(identifier: RPsiElement,
                  globalSearchScope: GlobalSearchScope = RSearchScopeUtil.getScope(identifier)): Array<ResolveResult> {
    val owner =
      if (RS4ContextProvider.getS4Context(identifier, RS4NewObjectSlotNameContext::class) != null) identifier
      else (identifier.parent as? RAtExpression)?.leftExpr ?: return emptyArray()
    val name = identifier.name ?: return emptyArray()
    val res = mutableListOf<ResolveResult>()
    val project = identifier.project
    findElementS4ClassDeclarations(owner, globalSearchScope).forEach { def ->
      val declClass = RS4ClassInfoUtil.getAllAssociatedSlots(def).firstOrNull { it.name == name }?.declarationClass ?: return@forEach
      findClassDeclarations(declClass, identifier, project, globalSearchScope).forEach { declarationClassDef ->
        val element = when (declarationClassDef) {
          is RSkeletonCallExpression -> RPomTarget.createSkeletonS4SlotTarget(declarationClassDef, name)
          else -> RS4ClassInfoUtil.findSlotInClassDefinition(declarationClassDef, name)?.let { (slot, def) ->
            when {
              def.name != slot.name -> RPomTarget.createS4ComplexSlotTarget(def, slot)
              def is RStringLiteralExpression -> RPomTarget.createStringLiteralTarget(def)
              else -> def
            }
          }
        }
        element?.let { res.add(PsiElementResolveResult(it)) }
      }
    }
    return res.toTypedArray()
  }

  fun resolveS4ClassName(className: RStringLiteralExpression,
                         globalSearchScope: GlobalSearchScope = RSearchScopeUtil.getScope(className)): Array<ResolveResult> {
    val name = className.name ?: return emptyArray()
    return findClassDeclarations(name, className, className.project, globalSearchScope).mapNotNull { call ->
      val decl =
        if (call is RSkeletonCallExpression) RPomTarget.createSkeletonS4ClassTarget(call)
        else RS4ClassInfoUtil.getAssociatedClassNameIdentifier(call)
      decl?.let { PsiElementResolveResult(it) }
    }.toTypedArray()
  }

  fun resolveS4GenericOrMethods(element: PsiElement,
                                name: String,
                                result: MutableList<ResolveResult>,
                                globalSearchScope: GlobalSearchScope) {
    val call = element.parent as? RCallExpression ?: return
    RS4GenericIndex.findDefinitionsByName(name, element.project, globalSearchScope).map { generic ->
      val genericInfo = generic.associatedS4GenericInfo ?: return@map generic
      val argumentInfo = RArgumentInfo.getArgumentInfo(call.argumentList, genericInfo.signature.parameters)
      val types = argumentInfo.toS4MethodParameters(false)

      val methodName = generic.methodName ?: return@map generic
      val methodsWithDistance = RS4MethodsIndex.findDefinitionsByName(methodName, generic.project, globalSearchScope)
        .mapNotNull { def ->
          val info = def.associatedS4MethodInfo?.getParameters(globalSearchScope)
                       ?.sortedBy { it.name }
                       ?.takeIf { it.isNotEmpty() }
                     ?: return@mapNotNull null
          if (info.size != types.size || info.zip(types).any { (a, b) -> a.name != b.name }) return@mapNotNull null
          def to info.zip(types).map { (a, b) ->
            RS4ClassInfoUtil.lookupDistanceBetweenClasses(b.type, a.type, element.project, globalSearchScope)
          }
        }
      val anyDistance = methodsWithDistance.maxOfOrNull { methodWithDistance ->
        methodWithDistance.second.maxOfOrNull { if (it is RS4OrdDistance) it.distance else 0 } ?: 0
      } ?: 0 + 1
      methodsWithDistance.map { methodWithDistance ->
        methodWithDistance.first to methodWithDistance.second.sumOf {
          when (it) {
            RS4DistanceToANY -> anyDistance
            RS4InfDistance -> 100_000
            is RS4OrdDistance -> it.distance
          }
        }
      }.minByOrNull { it.second }?.first ?: generic
    }.forEach {
      val target = when (val methodNameIdentifier = it.methodNameIdentifier) {
        null -> it
        is RStringLiteralExpression -> RPomTarget.createStringLiteralTarget(methodNameIdentifier)
        else -> methodNameIdentifier
      }
      result.add(PsiElementResolveResult(target))
    }
  }

  fun findElementS4ClassDeclarations(element: RPsiElement,
                                     globalSearchScope: GlobalSearchScope = RSearchScopeUtil.getScope(element)): List<RCallExpression> {
    val classNames = RS4TypeResolver.resolveS4TypeClass(element)
    val project = element.project
    return classNames.flatMap { findClassDeclarations(it, element, project, globalSearchScope) }
  }

  private fun findClassDeclarations(className: String,
                                    element: PsiElement,
                                    project: Project,
                                    scope: GlobalSearchScope?): Collection<RCallExpression> {
    val staticDefs = RS4ClassNameIndex.findClassDefinitions(className, project, scope)
    return staticDefs.ifEmpty {
      RS4SourceManager.getSourceCallFromInterop(element.containingFile.runtimeInfo, className)?.let { listOf(it) } ?: emptyList()
    }
  }
}