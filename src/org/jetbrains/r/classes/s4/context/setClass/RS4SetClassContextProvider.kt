package org.jetbrains.r.classes.s4.context.setClass

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.s4.classInfo.RSkeletonS4ClassPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.api.RStringLiteralExpression
import org.jetbrains.r.psi.isFunctionFromLibrary

sealed class RS4SetClassTypeContext : RS4SetClassContext()

// setClass("<caret>")
data class RS4SetClassClassNameContext(override val originalElement: RPsiElement,
                                       override val contextFunctionCall: RCallExpression) : RS4SetClassTypeContext()

sealed class RS4SetClassTypeUsageContext : RS4SetClassTypeContext()

// setClass("MyClass", "<caret>")
data class RS4SetClassRepresentationContext(override val originalElement: RPsiElement,
                                            override val contextFunctionCall: RCallExpression) : RS4SetClassTypeUsageContext()

// setClass("MyClass", , , "<caret>")
data class RS4SetClassContainsContext(override val originalElement: RPsiElement,
                                      override val contextFunctionCall: RCallExpression) : RS4SetClassTypeUsageContext()

// setClass("MyClass", contains = "<caret>")
// setClass("MyClass", contains = c("<caret>"))
// setClass("MyClass", contains = c(smt = "<caret>")
// setClass("MyClass", representation = representation(smt = "<caret>")
// setClass("MyClass", representation = representation("<caret>")
// setClass("MyClass", slots = c(name = "<caret>"))
data class RS4SetClassDependencyClassNameContext(override val originalElement: RPsiElement,
                                                 override val contextFunctionCall: RCallExpression) : RS4SetClassTypeUsageContext()

class RS4SetClassTypeContextProvider : RS4ContextProvider<RS4SetClassTypeContext>() {
  override fun getS4ContextWithoutCaching(element: RPsiElement): RS4SetClassTypeContext? {
    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    return if (parentCall.isFunctionFromLibrary("setClass", "methods")) {
      val parentArgumentInfo = RParameterInfoUtil.getArgumentInfo(parentCall) ?: return null
      when (element) {
        parentArgumentInfo.getArgumentPassedToParameter("Class") -> {
          // setClass("<caret>")
          if (element is RStringLiteralExpression) RS4SetClassClassNameContext(element, parentCall)
          else null
        }
        parentArgumentInfo.getArgumentPassedToParameter("representation") -> {
          // setClass("MyClass", "<caret>")
          RS4SetClassRepresentationContext(element, parentCall)
        }
        parentArgumentInfo.getArgumentPassedToParameter("contains") -> {
          // setClass("MyClass", , , "<caret>")
          RS4SetClassContainsContext(element, parentCall)
        }
        else -> null
      }
    }
    else {
      val grandParentCall = PsiTreeUtil.getParentOfType(parentCall, RCallExpression::class.java) ?: return null
      if (grandParentCall.isFunctionFromLibrary("setClass", "methods")) {
        val grandParentArgumentInfo = RParameterInfoUtil.getArgumentInfo(grandParentCall) ?: return null
        return when {
          // setClass("MyClass", contains = "<caret>")
          // setClass("MyClass", contains = c("<caret>"))
          // setClass("MyClass", contains = c(smt = "<caret>")
          // setClass("MyClass", representation = representation(smt = "<caret>")
          // setClass("MyClass", representation = representation("<caret>")
          PsiTreeUtil.isAncestor(grandParentArgumentInfo.getArgumentPassedToParameter("contains"), element, false) ||
          PsiTreeUtil.isAncestor(grandParentArgumentInfo.getArgumentPassedToParameter("representation"), element, false) -> {
            val parent = element.parent
            if (parent is RNamedArgument && parent.nameIdentifier == element) null
            else RS4SetClassDependencyClassNameContext(element, grandParentCall)
          }

          // setClass("MyClass", slots = c(name = "<caret>"))
          PsiTreeUtil.isAncestor(grandParentArgumentInfo.getArgumentPassedToParameter("slots"), element, false) -> {
            val parent = element.parent
            if (parent !is RNamedArgument || parent.assignedValue != element) null
            else RS4SetClassDependencyClassNameContext(element, grandParentCall)
          }
          else -> null
        }
      }
      else {
        val grandGrandParentCall = PsiTreeUtil.getParentOfType(grandParentCall, RCallExpression::class.java) ?: return null
        if (!grandGrandParentCall.isFunctionFromLibrary("setClass", "methods")) return null
        val grandGrandParentArgumentInfo = RParameterInfoUtil.getArgumentInfo(grandGrandParentCall) ?: return null

        // setClass("MyClass", slots = c(name = c("<caret>")))
        // setClass("MyClass", slots = c(name = c(ext = "<caret>")))
        if (PsiTreeUtil.isAncestor(grandGrandParentArgumentInfo.getArgumentPassedToParameter("slots"), element, false)) {
          val parent = element.parent
          if (parent is RNamedArgument && parent.nameIdentifier == element) null
          else RS4SetClassDependencyClassNameContext(element, grandGrandParentCall)
        }
        else null
      }
    }
  }

  override fun getS4ContextForPomTarget(element: PomTargetPsiElement): RS4SetClassTypeContext? {
    return when (val target = element.target) {
      is RSkeletonS4ClassPomTarget -> RS4SetClassClassNameContext(element as RPsiElement, target.setClass)
      else -> null
    }
  }
}
