/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.classes.s4.RStringLiteralPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.RRefactoringUtil

class RenameRPsiElementProcessor : RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean = element.language == RLanguage.INSTANCE

  override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
    if (RPsiUtil.isLibraryElement(element)) return element
    return when (element) {
      is RIdentifierExpression -> {
        val parent = element.parent
        when {
          parent is RForStatement && parent.target == element -> element
          parent is RAssignmentStatement || parent is RParameter || parent is RNamedArgument -> parent
          else -> null
        }
      }
      is RForStatement -> element.target
      is RAssignmentStatement, is RParameter, is RNamedArgument, is RFile -> element
      is PomTargetPsiElement -> {
        val target = element.target
        if (target is RStringLiteralPomTarget) target.literal
        else null
      }
      is RStringLiteralExpression -> {
        val context = RS4ContextProvider.getS4Context(element, RS4SetClassClassNameContext::class)
        if (context != null) element else null
      }
      else -> null
    }
  }

  override fun findCollisions(element: PsiElement,
                              newName: String,
                              allRenames: MutableMap<out PsiElement, String>,
                              result: MutableList<UsageInfo>) {
    fun findCollisionsInner(element: RPsiElement) {
      val scope = RRefactoringUtil.getRScope(element)
      val stringScope = if (scope is RFunctionExpression) {
        val name = (scope.parent as? RAssignmentStatement)?.name ?: RBundle.message("rename.processor.function.no.name")
        RBundle.message("rename.processor.function.scope", name)
      }
      else {
        RBundle.message("rename.processor.file.scope", (scope as RFile).name)
      }

      scope.getLocalVariableInfo(scope.controlFlow.instructions.last())?.variables?.get(newName)?.let { variableDefinition ->
        val definition = variableDefinition.variableDescription.firstDefinition.let {
          (it as? RExpression)?.let { RPsiUtil.getAssignmentByAssignee(it) } ?: it
        }
        val description = if (definition is RAssignmentStatement && definition.isFunctionDeclaration) {
          RBundle.message("rename.processor.collision.function.description", newName, stringScope)
        }
        else {
          RBundle.message("rename.processor.collision.variable.description", newName, stringScope)
        }
        result.add(RUnresolvableCollisionUsageInfo(definition, definition, description))
      }
    }

    (element as? RPsiElement)?.let { findCollisionsInner(it) }
    result.mapNotNull { it.element as? RPsiElement }.forEach { findCollisionsInner(it) }
  }

  private class RUnresolvableCollisionUsageInfo(element: PsiElement,
                                                referencedElement: PsiElement,
                                                private val description: String)
    : UnresolvableCollisionUsageInfo(element, referencedElement) {
    override fun getDescription() = description
  }
}