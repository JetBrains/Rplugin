// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*

object RDebuggerUtils {
  fun getFunctionNameByText(text: String, project: Project): String {
    return ApplicationManager.getApplication().runReadAction(
      Computable<String> { getFunctionName(RElementFactory.createRPsiElementFromText(project, text)) }
    )
  }

  private fun getFunctionName(element: PsiElement?): String {
    var elem = element
    var wasCall = false
    while (true) {
      when (elem) {
        is RIdentifierExpression -> return elem.name
        is RNamespaceAccessExpression -> return "${elem.namespaceName}::${elem.identifier?.name.orEmpty()}"
        is ROperator -> return elem.name
        is RCallExpression -> {
          if (wasCall) return RBundle.message("debugger.anonymous.stack.frame")
          elem = elem.expression
          wasCall = true
        }
        is ROperatorExpression -> {
          if (wasCall) return RBundle.message("debugger.anonymous.stack.frame")
          wasCall = true
          elem = elem.operator
        }
        is RParenthesizedExpression -> elem = elem.expression
        else -> return RBundle.message("debugger.anonymous.stack.frame")
      }
    }
  }
}
