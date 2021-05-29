/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.classes.s4.context.setClass

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil.toComplexSlots
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil.toSlot
import org.jetbrains.r.classes.s4.classInfo.RS4ClassSlot
import org.jetbrains.r.classes.s4.classInfo.RSkeletonS4SlotPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrarySoft

class RS4SlotDeclarationContext(override val originalElement: RPsiElement,
                                override val contextFunctionCall: RCallExpression,
                                val slots: List<RS4ClassSlot>) : RS4SetClassContext() {
  constructor(originalElement: RPsiElement, contextFunctionCall: RCallExpression, slot: RS4ClassSlot) :
    this(originalElement, contextFunctionCall, listOf(slot))
}

class RS4SlotDeclarationContextProvider : RS4ContextProvider<RS4SlotDeclarationContext>() {
  @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
  override fun getS4ContextWithoutCaching(_element: RPsiElement): RS4SlotDeclarationContext? {
    val element =
      if (_element is RIdentifierExpression) _element.parent
      else _element
    if (element !is RStringLiteralExpression && element !is RNamedArgument) return null

    val parentCall = PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: return null
    return if (parentCall.isFunctionFromLibrarySoft("setClass", "methods")) {
      if (element !is RStringLiteralExpression) return null
      val parentArgumentInfo = RArgumentInfo.getArgumentInfo(parentCall) ?: return null
      val className = RS4ClassInfoUtil.getAssociatedClassName(parentCall, parentArgumentInfo) ?: return null
      when (element) {
        parentArgumentInfo.getArgumentPassedToParameter("slots") -> {
          val slot = element.toSlot(className) ?: return null
          RS4SlotDeclarationContext(element, parentCall, slot)
        }
        else -> null
      }
    }
    else {
      val superParentCall = PsiTreeUtil.getParentOfType(parentCall, RCallExpression::class.java) ?: return null
      if (!superParentCall.isFunctionFromLibrarySoft("setClass", "methods")) return null

      val superParentArgumentInfo = RArgumentInfo.getArgumentInfo(superParentCall) ?: return null
      val className = RS4ClassInfoUtil.getAssociatedClassName(superParentCall, superParentArgumentInfo) ?: return null
      return when {
        PsiTreeUtil.isAncestor(superParentArgumentInfo.getArgumentPassedToParameter("slots"), element, false) -> {
          val slots = when (element) {
            is RStringLiteralExpression -> element.toSlot(className)?.let { listOf(it) } ?: return null
            is RNamedArgument -> element.toComplexSlots(className)
            else -> return null
          }

          RS4SlotDeclarationContext(element as RPsiElement, superParentCall, slots)
        }
        else -> null
      }
    }
  }

  override fun getS4ContextForPomTarget(element: PomTargetPsiElement): RS4SlotDeclarationContext? {
    return when (val target = element.target) {
      is RSkeletonS4SlotPomTarget -> {
        val slot = target.setClass.associatedS4ClassInfo.slots.firstOrNull { it.name == target.name} ?: return null
        RS4SlotDeclarationContext(element as RPsiElement, target.setClass, slot)
      }
      else -> null
    }
  }
}