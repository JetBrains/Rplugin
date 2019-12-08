/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.usageView.UsageInfo
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.refactoring.RRefactoringUtil

class RenameRPsiElementProcessor : RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean = element.language == RLanguage.INSTANCE

  override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
    if (RPsiUtil.isLibraryElement(element)) return null

    if (element is RIdentifierExpression) {
      val parent = element.parent
      return when {
        parent is RForStatement && parent.target == element -> element
        parent is RAssignmentStatement || parent is RParameter -> parent
        else -> null
      }
    }

    if (element is RForStatement) return element.target
    if (element is RAssignmentStatement || element is RParameter) return element
    return null
  }

  override fun findCollisions(element: PsiElement,
                              newName: String,
                              allRenames: MutableMap<out PsiElement, String>,
                              result: MutableList<UsageInfo>) {
    val scope = RRefactoringUtil.getRScope(element)
    val stringScope = if (scope is RFunctionExpression) {
      val name = (scope.parent as? RAssignmentStatement)?.name ?: RBundle.message("rename.processor.function.no.name")
      RBundle.message("rename.processor.function.scope", name)
    } else {
      RBundle.message("rename.processor.file.scope", (scope as RFile).name)
    }

    fun addResult(element: PsiElement) {
      val description = if (element is RAssignmentStatement && element.isFunctionDeclaration) {
        RBundle.message("rename.processor.collision.function.description", newName, stringScope)
      } else {
        RBundle.message("rename.processor.collision.variable.description", newName, stringScope)
      }
      result.add(RUnresolvableCollisionUsageInfo(element, element, description))
    }

    scope.accept(object : RRecursiveElementVisitor() {
      override fun visitAssignmentStatement(o: RAssignmentStatement) {
        val name = (o.assignee as? RIdentifierExpression)?.name ?: return
        if (name == newName) addResult(o)
      }

      override fun visitParameter(o: RParameter) {
        if (o.name == newName) addResult(o)
      }
    })
  }

  private class RUnresolvableCollisionUsageInfo(element: PsiElement,
                                                referencedElement: PsiElement,
                                                private val description: String)
    : UnresolvableCollisionUsageInfo(element, referencedElement) {
    override fun getDescription() = description
  }
}