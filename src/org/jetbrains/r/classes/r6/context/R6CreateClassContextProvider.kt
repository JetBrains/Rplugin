/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.r6.context

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.common.context.LibraryClassContext
import org.jetbrains.r.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class R6CreateClassContext : LibraryClassContext {
  override val functionName = "R6Class"
}

// Accumulator <- R6Class("Accumulator", ...)
data class R6CreateClassNameContext(override val originalElement: RPsiElement,
                                    override val functionCall: RCallExpression,
                                    override val argumentInfo: RArgumentInfo) : R6CreateClassContext()

class R6CreateClassContextProvider : R6ContextProvider<R6CreateClassContext>() {
  override fun getContext(element: RPsiElement): R6CreateClassContext? {
    return CachedValuesManager.getCachedValue(element) {
      CachedValueProvider.Result.create(getR6ContextInner(element), element)
    }
  }

  private fun getR6ContextInner(element: RPsiElement): R6CreateClassContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    return if (parentCall.isFunctionFromLibrary(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName)) {
      val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null
      when (element) {
//        parentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentClassName) -> {
//          // R6Class("<caret>")
//          R6CreateClassNameContext(element, parentCall, parentArgumentInfo)
//        }
        parentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentSuperClass) -> {
          // R6Class("MyClass", "<caret>")
          R6CreateClassNameContext(element, parentCall, parentArgumentInfo)
        }
        parentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPublic) -> {
          // R6Class("MyClass", , "<caret>")
          R6CreateClassNameContext(element, parentCall, parentArgumentInfo)
        }
        parentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPrivate) -> {
          // R6Class("MyClass", , , "<caret>")
          R6CreateClassNameContext(element, parentCall, parentArgumentInfo)
        }
        parentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentActive) -> {
          // R6Class("MyClass", , , , "<caret>")
          R6CreateClassNameContext(element, parentCall, parentArgumentInfo)
        }
        else -> null
      }
    }
    else {
      val superParentCall = PsiTreeUtil.getParentOfType(parentCall, RCallExpression::class.java) ?: return null
      if (!superParentCall.isFunctionFromLibrary(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName)) return null
      val superParentArgumentInfo = RParameterInfoUtil.getArgumentInfo(superParentCall) ?: return null

      return when {
        // R6Class("MyClass", inherit = "<caret>"
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentSuperClass), element, false) -> {
          val parent = element.parent
          if (parent is RNamedArgument && parent.nameIdentifier == element) null
          else R6CreateClassNameContext(element, superParentCall, superParentArgumentInfo)
        }

        // R6Class("MyClass", public = "<caret>"
        // R6Class("MyClass", public = list("<caret>")
        // R6Class("MyClass", public = list(smt = "<caret>")

        // R6Class("MyClass", private = "<caret>"
        // R6Class("MyClass", private = list("<caret>")
        // R6Class("MyClass", private = list(smt = "<caret>")

        // R6Class("MyClass", active = "<caret>"
        // R6Class("MyClass", active = list("<caret>")
        // R6Class("MyClass", active = list(smt = "<caret>")
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPublic), element, false) ||
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentPrivate), element, false) ||
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter(R6ClassInfoUtil.argumentActive), element, false) -> {
          val parent = element.parent
          if (parent !is RNamedArgument || parent.assignedValue != element) null
          else R6CreateClassNameContext(element, superParentCall, superParentArgumentInfo)
        }

        else -> null
      }
    }
  }
}