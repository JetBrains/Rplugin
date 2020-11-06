/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.psi.api.RArgumentList
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.references.RReferenceBase

class RSkeletonCallExpression(private val myStub: RSkeletonCallExpressionStub) : RSkeletonBase(), RCallExpression {
  override fun getMirror() = null

  override fun getParent(): PsiElement = myStub.parentStub.psi

  override fun getStub(): RSkeletonCallExpressionStub = myStub

  override fun getExpression(): RExpression {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getElementType(): IStubElementType<out StubElement<*>, *> = stub.stubType

  override fun getName(): String = myStub.s4ClassInfo.className

  override fun canNavigate(): Boolean = false

  override fun getText(): String {
    val info = stub.s4ClassInfo
    return buildString {
      append("setClass('").append(info.className).append("', ")
      append("slots = c(")
      info.slots.forEachIndexed { ind, slot ->
        if (ind != 0) append(", ")
        append(slot.name).append(" = '").append(slot.type).append("'")
      }
      append("), ")
      append("contains = c(")
      info.superClasses.forEachIndexed { ind, superClass ->
        if (ind != 0) append(", ")
        append("'").append(superClass).append("'")
      }
      if (info.isVirtual) {
        if (info.superClasses.isNotEmpty()) append(", ")
        append("'VIRTUAL'")
      }
      append("))")
    }
  }

  override fun getArgumentList(): RArgumentList {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getAssociatedS4ClassInfo(): RS4ClassInfo = myStub.s4ClassInfo

  override fun getReference(): RReferenceBase<*>? = null

  override fun navigate(requestFocus: Boolean) {}
}