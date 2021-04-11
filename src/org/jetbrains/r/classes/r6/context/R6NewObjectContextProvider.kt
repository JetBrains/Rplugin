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
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class R6NewObjectContext : LibraryClassContext {
  override val functionName = "new"
}

// Calculator <- Accumulator$new()
data class R6NewObjectClassNameContext(override val originalElement: RPsiElement,
                                       override val functionCall: RCallExpression,
                                       override val argumentInfo: RArgumentInfo) : R6NewObjectContext()

class R6NewObjectContextProvider : R6ContextProvider<R6NewObjectContext>() {
  override fun getContext(element: RPsiElement): R6NewObjectContext? {
    return CachedValuesManager.getCachedValue(element) {
      CachedValueProvider.Result.create(getR6ContextInner(element), element)
    }
  }

  private fun getR6ContextInner(element: RPsiElement): R6NewObjectContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    if (!parentCall.isFunctionFromLibrary(R6ClassInfoUtil.functionNew, R6ClassInfoUtil.R6PackageName)) return null
    val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null

    return null

//    return if (parentArgumentInfo.getArgumentPassedToParameter() == element) {
//      // MyClass$new()
//      RS4NewObjectClassNameContext(element, parentCall, parentArgumentInfo)
//    }
//    else {
//      val currentArgument =
//        if (element.parent is RNamedArgument) RPsiUtil.getNamedArgumentByNameIdentifier(element) ?: return null
//        else element
//      val arguments = parentCall.argumentList.expressionList
//      if (!arguments.contains(currentArgument)) return null
//      else {
//        // new("ClassName", slot<caret>_name)
//        // new("ClassName", slot<caret>_name = slot_value)
//        RS4NewObjectSlotNameContext(currentArgument, parentCall, parentArgumentInfo)
//      }
//    }
  }
}