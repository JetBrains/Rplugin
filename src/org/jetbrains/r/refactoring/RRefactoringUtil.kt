/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RControlFlowHolder
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.refactoring.inline.RInlineUtil

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

    scope.acceptChildren(object : RInlineUtil.RRecursiveElementVisitor() {
      override fun visitAssignmentStatement(o: RAssignmentStatement) {
        assignments.add(o)
      }
    })
    return assignments
  }

  fun getUniqueName(baseName: String, unavailableNames: MutableSet<String>): String {
    if (baseName !in unavailableNames) {
      unavailableNames.add(baseName)
      return baseName
    }

    var i = 1
    while ("$baseName$i" in unavailableNames) {
      ++i
    }
    val name = "$baseName$i"
    unavailableNames.add(name)
    return name
  }
}