package org.jetbrains.r.classes.s4.context

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.common.context.LibraryClassContext
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class RS4NewObjectContext : LibraryClassContext {
  override val functionName = "new"
}

// new("<caret>")
data class RS4NewObjectClassNameContext(override val originalElement: RPsiElement,
                                        override val functionCall: RCallExpression,
                                        override val argumentInfo: RArgumentInfo) : RS4NewObjectContext()

// new("ClassName", slot<caret>_name)
// new("ClassName", slot<caret>_name = slot_value)
data class RS4NewObjectSlotNameContext(override val originalElement: RPsiElement,
                                       override val functionCall: RCallExpression,
                                       override val argumentInfo: RArgumentInfo) : RS4NewObjectContext()

class RS4NewObjectContextProvider : RS4ContextProvider<RS4NewObjectContext>() {
  override fun getContext(element: RPsiElement): RS4NewObjectContext? {
    return CachedValuesManager.getCachedValue(element) {
      CachedValueProvider.Result.create(getS4ContextInner(element), element)
    }
  }

  private fun getS4ContextInner(element: RPsiElement): RS4NewObjectContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    if (!parentCall.isFunctionFromLibrary("new", "methods")) return null
    val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null
    return if (parentArgumentInfo.getArgumentPassedToParameter("Class") == element) {
      // new("<caret>")
      RS4NewObjectClassNameContext(element, parentCall, parentArgumentInfo)
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
        RS4NewObjectSlotNameContext(currentArgument, parentCall, parentArgumentInfo)
      }
    }
  }
}