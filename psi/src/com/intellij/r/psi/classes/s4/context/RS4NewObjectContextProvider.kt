/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.classes.s4.context

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.hints.parameterInfo.RArgumentInfo
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.isFunctionFromLibrarySoft

sealed class RS4NewObjectContext : RS4Context {
  override val functionName: String = "new"
}

// new("<caret>")
data class RS4NewObjectClassNameContext(override val originalElement: RPsiElement,
                                        override val functionCall: RCallExpression) : RS4NewObjectContext()

// new("ClassName", slot<caret>_name)
// new("ClassName", slot<caret>_name = slot_value)
data class RS4NewObjectSlotNameContext(override val originalElement: RPsiElement,
                                       override val functionCall: RCallExpression) : RS4NewObjectContext()

class RS4NewObjectContextProvider : RS4ContextProvider<RS4NewObjectContext>() {
  override fun getS4ContextWithoutCaching(element: RPsiElement): RS4NewObjectContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    if (!parentCall.isFunctionFromLibrarySoft("new", "methods")) return null
    val parentArgumentInfo = RArgumentInfo.getArgumentInfo(parentCall) ?: return null
    return if (parentArgumentInfo.getArgumentPassedToParameter("Class") == element) {
      // new("<caret>")
      RS4NewObjectClassNameContext(element, parentCall)
    }
    else {
      val currentArgument =
        if (element.parent is RNamedArgument) RPsiUtil.getNamedArgumentByNameIdentifier(element) ?: return null
        else element
      val arguments = parentCall.argumentList.expressionList
      if (!arguments.contains(currentArgument)) return null
      else {
        // new("ClassName", slot<caret>_name)
        // new("ClassName", slot<caret>_name = slot_value)
        RS4NewObjectSlotNameContext(currentArgument, parentCall)
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is RS4ContextProvider<*>) return false
    return this::class.java.name == other::class.java.name
  }
}