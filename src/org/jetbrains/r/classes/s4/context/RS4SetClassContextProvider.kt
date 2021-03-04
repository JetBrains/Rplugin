package org.jetbrains.r.classes.s4.context

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class RS4SetClassContext : RS4Context {
  override val functionName = "setClass"
}

// setClass("MyClass", "<caret>")
data class RS4SetClassRepresentationContext(override val originalElement: RPsiElement,
                                            override val functionCall: RCallExpression,
                                            override val argumentInfo: RArgumentInfo) : RS4SetClassContext()

// setClass("MyClass", , , "<caret>")
data class RS4SetClassContainsContext(override val originalElement: RPsiElement,
                                      override val functionCall: RCallExpression,
                                      override val argumentInfo: RArgumentInfo) : RS4SetClassContext()

// setClass("MyClass", contains = "<caret>")
// setClass("MyClass", contains = c("<caret>"))
// setClass("MyClass", contains = c(smt = "<caret>")
// setClass("MyClass", representation = representation(smt = "<caret>")
// setClass("MyClass", representation = representation("<caret>")
// setClass("MyClass", slots = c(name = "<caret>"))
data class RS4SetClassDependencyClassNameContext(override val originalElement: RPsiElement,
                                                 override val functionCall: RCallExpression,
                                                 override val argumentInfo: RArgumentInfo) : RS4SetClassContext()

class RS4SetClassContextProvider : RS4ContextProvider<RS4SetClassContext>() {
  override fun getS4Context(element: RPsiElement): RS4SetClassContext? {
    return CachedValuesManager.getCachedValue(element) {
      CachedValueProvider.Result.create(getS4ContextInner(element), element)
    }
  }

  private fun getS4ContextInner(element: RPsiElement): RS4SetClassContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    return if (parentCall.isFunctionFromLibrary("setClass", "methods")) {
      val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null
      when (element) {
        parentArgumentInfo.getArgumentPassedToParameter("representation") -> {
          // setClass("MyClass", "<caret>")
          RS4SetClassRepresentationContext(element, parentCall, parentArgumentInfo)
        }
        parentArgumentInfo.getArgumentPassedToParameter("contains") -> {
          // setClass("MyClass", , , "<caret>")
          RS4SetClassContainsContext(element, parentCall, parentArgumentInfo)
        }
        else -> null
      }
    }
    else {
      val superParentCall = PsiTreeUtil.getParentOfType(parentCall, RCallExpression::class.java) ?: return null
      if (!superParentCall.isFunctionFromLibrary("setClass", "methods")) return null

      val superParentArgumentInfo = RParameterInfoUtil.getArgumentInfo(superParentCall) ?: return null
      return when {
        // setClass("MyClass", contains = "<caret>")
        // setClass("MyClass", contains = c("<caret>"))
        // setClass("MyClass", contains = c(smt = "<caret>")
        // setClass("MyClass", representation = representation(smt = "<caret>")
        // setClass("MyClass", representation = representation("<caret>")
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter("contains"), element, false) ||
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter("representation"), element, false) -> {
          val parent = element.parent
          if (parent is RNamedArgument && parent.nameIdentifier == element) null
          else RS4SetClassDependencyClassNameContext(element, superParentCall, superParentArgumentInfo)
        }

        // setClass("MyClass", slots = c(name = "<caret>"))
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter("slots"), element, false) -> {
          val parent = element.parent
          if (parent !is RNamedArgument || parent.assignedValue != element) null
          else RS4SetClassDependencyClassNameContext(element, superParentCall, superParentArgumentInfo)
        }
        else -> null
      }
    }
  }
}
