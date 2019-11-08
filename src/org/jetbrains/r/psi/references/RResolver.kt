// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.interpreter.RInterpreterManager.Companion.getInstance
import org.jetbrains.r.psi.RPsiUtil.getFunction
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RParameterList
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex
import java.util.function.Predicate

object RResolver {
  internal val LOG = Logger.getInstance(
    "#" + RResolver::class.java.name)

  fun resolveWithNamespace(project: Project,
                           name: String,
                           namespace: String,
                           result: MutableList<ResolveResult>) {
    val interpreter = getInstance(project).interpreter ?: return
    val psiFile = interpreter.getSkeletonFileByPackageName(namespace) ?: return
    val statements = RAssignmentNameIndex.find(name, project, GlobalSearchScope.fileScope(psiFile))
    for (statement in statements) {
      if (statement.name == name) {
        result.add(PsiElementResolveResult(statement))
      }
    }
  }

  fun <T> not(t: Predicate<T>): Predicate<T> {
    return t.negate()
  }

  fun resolveNameArgument(element: PsiElement,
                          elementName: String,
                          result: MutableList<ResolveResult>) {
    val callExpression = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java)
    if (callExpression != null) {
      val functionExpression = getFunction(callExpression)
      val parameterList = PsiTreeUtil.getChildOfType(functionExpression, RParameterList::class.java)
      if (parameterList != null) {
        for (parameter in parameterList.parameterList) {
          if (parameter.name == elementName) {
            result.add(0, PsiElementResolveResult(parameter))
            return
          }
        }
      }
    }
  }

  fun resolveInFileOrLibrary(element: PsiElement,
                             name: String,
                             myResult: MutableList<ResolveResult>) {
    resolveFromStubs(element, myResult, name)
  }

  private fun resolveFromStubs(element: PsiElement,
                               result: MutableList<ResolveResult>,
                               name: String) {
    val statements = RAssignmentNameIndex.find(name, element.project, RSearchScopeUtil.getScope(element))
    addResolveResults(result, statements)
  }

  private fun addResolveResults(result: MutableList<ResolveResult>,
                                statements: Collection<RAssignmentStatement>) {
    for (statement in statements) {
      result.add(PsiElementResolveResult(statement))
    }
  }
}