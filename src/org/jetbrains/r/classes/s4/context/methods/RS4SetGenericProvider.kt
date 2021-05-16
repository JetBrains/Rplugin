/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context.methods

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class RS4SetGenericContext : RS4MethodsContext() {
  override val contextFunctionName: String = "setGeneric"
}

data class RS4SetGenericFunctionNameContext(override val originalElement: RPsiElement,
                                            override val contextFunctionCall: RCallExpression) : RS4SetGenericContext()

// setGeneric("fun", valueClass = "<caret>")
// setGeneric("fun", valueClass = c("<caret>"))
// setGeneric("fun", valueClass = c(unused = "<caret>"))
data class RS4SetGenericValueClassesContext(override val originalElement: RPsiElement,
                                            override val contextFunctionCall: RCallExpression) : RS4SetGenericContext()

// setGeneric("fun", signature = "<caret>")
// setGeneric("fun", signature = c("<caret>"))
// setGeneric("fun", signature = c(unused = "<caret>"))
data class RS4SetGenericParameterNamesContext(override val originalElement: RPsiElement,
                                              override val contextFunctionCall: RCallExpression) : RS4SetGenericContext()

class RS4SetGenericProvider : RS4ContextProvider<RS4SetGenericContext>() {
  override fun getS4ContextWithoutCaching(element: RPsiElement): RS4SetGenericContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    return if (parentCall.isFunctionFromLibrary("setGeneric", "methods")) {
      val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null
      when (element) {
        parentArgumentInfo.getArgumentPassedToParameter("name") -> {
          RS4SetGenericFunctionNameContext(element, parentCall)
        }
        parentArgumentInfo.getArgumentPassedToParameter("signature") -> {
          RS4SetGenericParameterNamesContext(element, parentCall)
        }
        parentArgumentInfo.getArgumentPassedToParameter("valueClass") -> {
          RS4SetGenericValueClassesContext(element, parentCall)
        }
        else -> null
      }
    }
    else {
      val grandParentCall = PsiTreeUtil.getParentOfType(parentCall, RCallExpression::class.java) ?: return null
      if (grandParentCall.isFunctionFromLibrary("setGeneric", "methods")) {
        val grandParentArgumentInfo = RParameterInfoUtil.getArgumentInfo(grandParentCall) ?: return null
        return when {
          PsiTreeUtil.isAncestor(grandParentArgumentInfo.getArgumentPassedToParameter("signature"), element, false) -> {
            val parent = element.parent
            if (parent is RNamedArgument && parent.nameIdentifier == element) null
            else RS4SetGenericParameterNamesContext(element, grandParentCall)
          }
          PsiTreeUtil.isAncestor(grandParentArgumentInfo.getArgumentPassedToParameter("valueClass"), element, false) -> {
            val parent = element.parent
            if (parent is RNamedArgument && parent.nameIdentifier == element) null
            else RS4SetGenericValueClassesContext(element, grandParentCall)
          }
          else -> null
        }
      }
      else null
    }
  }
}
