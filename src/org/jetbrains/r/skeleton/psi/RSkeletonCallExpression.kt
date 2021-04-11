/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.classes.r6.R6ClassInfo
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

  override fun getName(): String = myStub.s4ClassInfo?.className ?: ""

  override fun canNavigate(): Boolean = false

  override fun getText(): String {
    val s4Info = stub.s4ClassInfo
    val r6Info = stub.r6ClassInfo

    return buildString {
      if (s4Info != null) { append(s4Info.toString()) }
      if (r6Info != null) { append(r6Info.toString()) }
    }
  }

  override fun getArgumentList(): RArgumentList {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getAssociatedS4ClassInfo(): RS4ClassInfo? = myStub.s4ClassInfo

  override fun getAssociatedR6ClassInfo(): R6ClassInfo? = myStub.r6ClassInfo

  override fun getReference(): RReferenceBase<*>? = null

  override fun navigate(requestFocus: Boolean) {}
}