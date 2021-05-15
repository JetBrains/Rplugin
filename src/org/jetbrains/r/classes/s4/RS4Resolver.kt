package org.jetbrains.r.classes.s4

import com.intellij.openapi.project.Project
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil
import org.jetbrains.r.classes.s4.classInfo.RS4ComplexSlotPomTarget
import org.jetbrains.r.classes.s4.classInfo.RSkeletonS4SlotPomTarget
import org.jetbrains.r.classes.s4.classInfo.RStringLiteralPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.RS4NewObjectSlotNameContext
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.references.RSearchScopeUtil
import org.jetbrains.r.psi.stubs.RS4ClassNameIndex
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpression

object RS4Resolver {

  fun resolveSlot(identifier: RPsiElement): Array<ResolveResult> {
    val owner =
      if (RS4ContextProvider.getS4Context(identifier, RS4NewObjectSlotNameContext::class)!= null) identifier
      else (identifier.parent as? RAtExpression) ?.leftExpr ?: return emptyArray()
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

  fun findElementS4ClassDeclarations(element: RPsiElement): List<RCallExpression> {
    val classNames =
      when (element) {
        is RIdentifierExpression -> {
          val newSlotContext = RS4ContextProvider.getS4Context(element, RS4NewObjectSlotNameContext::class)
          if (newSlotContext != null) {
            RS4ClassInfoUtil.getAssociatedClassName(newSlotContext.contextFunctionCall)?.let { listOf(it) }
          }
          else element.reference.multiResolve(false).mapNotNull {
            val assignment = it.element as? RAssignmentStatement ?: return@mapNotNull null
            val definition = assignment.assignedValue as? RCallExpression ?: return@mapNotNull null
            RS4ClassInfoUtil.getAssociatedClassName(definition) ?:
              element.containingFile.runtimeInfo?.loadS4ClassInfoByObjectName(assignment.name)?.className
          }
        }
        is RAtExpression -> {
          val ownerIdentifier = element.rightExpr as? RIdentifierExpression ?: return emptyList()
          ownerIdentifier.reference.multiResolve(false).mapNotNull { resolveResult ->
            when (val resolveElement = resolveResult.element) {
              is RNamedArgument -> resolveElement.assignedValue?.name
              is PomTargetPsiElement -> {
                when (val target = resolveElement.target) {
                  is RSkeletonS4SlotPomTarget -> target.setClass.stub.s4ClassInfo.slots.firstOrNull { it.name == target.name }?.type
                  is RS4ComplexSlotPomTarget -> target.slot.type
                  is RStringLiteralPomTarget -> "ANY"
                  else -> null
                }
              }
              else -> null
            }
          }
        }
        else -> null
      } ?: return emptyList()

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