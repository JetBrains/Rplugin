/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.RS4NewObjectSlotNameContext
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4GenericInfo
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.associatedS4MethodInfo
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.methodName
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.methodNameIdentifier
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil.toS4MethodParameters
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.RAtExpression
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RS4ClassNameIndex
import org.jetbrains.r.psi.stubs.RS4GenericIndex
import org.jetbrains.r.psi.stubs.RS4MethodsIndex
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

object RS4Resolver {

  fun resolveSlot(identifier: RPsiElement): Array<ResolveResult> {
    val owner =
      if (RS4ContextProvider.getS4Context(identifier, RS4NewObjectSlotNameContext::class) != null) identifier
      else (identifier.parent as? RAtExpression)?.leftExpr ?: return emptyArray()
    val name = identifier.name ?: return emptyArray()
    val res = mutableListOf<ResolveResult>()
    val project = identifier.project
    val scope = RSearchScopeUtil.getScope(identifier)
    findElementS4ClassDeclarations(owner).forEach { def ->
      val declClass = RS4ClassInfoUtil.getAllAssociatedSlots(def).firstOrNull { it.name == name }?.declarationClass ?: return@forEach
      findClassDeclarations(declClass, identifier, project, scope).forEach { declarationClassDef ->
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

  fun resolveS4ClassName(className: RStringLiteralExpression): Array<ResolveResult> {
    val name = className.name ?: return emptyArray()
    return findClassDeclarations(name, className, className.project, RSearchScopeUtil.getScope(className)).mapNotNull { call ->
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
      val argumentInfo = RArgumentInfo.getParameterInfo(call.argumentList, genericInfo.signature.parameters)
      val types = argumentInfo.toS4MethodParameters(false)

      val methodName = generic.methodName ?: return@map generic
      RS4MethodsIndex.findDefinitionsByName(methodName, generic.project, globalSearchScope)
        .mapNotNull { def ->
          val info = def.associatedS4MethodInfo?.getParameters(globalSearchScope)?.sortedBy { it.name } ?: return@mapNotNull null
          def.takeIf {
            if (info.size != types.size) return@takeIf false
            info.zip(types).all { (a, b) ->
              a.name == b.name &&
              RS4ClassInfoUtil.isSubclass(b.type, a.type, element.project, globalSearchScope)
            }
          }
        }
        .firstOrNull() ?: generic
    }.forEach {
      val target = when (val methodNameIdentifier = it.methodNameIdentifier) {
        null -> it
        is RStringLiteralExpression -> RPomTarget.createStringLiteralTarget(methodNameIdentifier)
        else -> methodNameIdentifier
      }
      result.add(PsiElementResolveResult(target))
    }
  }

  fun findElementS4ClassDeclarations(element: RPsiElement): List<RCallExpression> {
    val classNames = RS4TypeResolver.resolveS4TypeClass(element)
    val project = element.project
    val scope = RSearchScopeUtil.getScope(element)
    return classNames.flatMap { findClassDeclarations(it, element, project, scope) }
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