/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.*

object RRefactoringUtil {
  fun getRScope(local: PsiElement): RControlFlowHolder {
    var context: RControlFlowHolder? = PsiTreeUtil.getParentOfType(local, RFunctionExpression::class.java)
    if (context == null) {
      context = local.containingFile as RFile
    }
    return context
  }

  fun collectUsedNames(scope: PsiElement?): Collection<String> {
    return collectAssignments(scope).map { it.name }
  }

  fun collectAssignments(scope: PsiElement?): Collection<RAssignmentStatement> {
    if (scope == null) return emptyList()
    val assignments = mutableSetOf<RAssignmentStatement>()

    scope.acceptChildren(object : RVisitor(), PsiRecursiveVisitor {
      override fun visitElement(element: PsiElement) {
        if (element !is RFunctionExpression)
          element.acceptChildren(this)
      }

      override fun visitAssignmentStatement(o: RAssignmentStatement) {
        assignments.add(o)
      }
    })
    return assignments
  }

  fun getUniqueName(baseName: String, unavailableNames: MutableSet<String>, isFunctionName: Boolean = false): String {
    if (baseName !in unavailableNames) {
      unavailableNames.add(baseName)
      return baseName
    }

    var i = 1
    val name = if (isFunctionName && baseName.contains(".")) {
      while (baseName.replaceFirst(".", "$i.") in unavailableNames) {
        ++i
      }
      baseName.replaceFirst(".", "$i.")
    }
    else {
      while ("$baseName$i" in unavailableNames) {
        ++i
      }
      "$baseName$i"
    }
    unavailableNames.add(name)
    return name
  }
}