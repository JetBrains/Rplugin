/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.r.psi.psi.api.RArgumentList
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.references.RReferenceBase
import com.intellij.util.IncorrectOperationException

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
    return stub.s4ClassInfo.getDeclarationText(project)
  }

  override fun getArgumentList(): RArgumentList {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getReference(): RReferenceBase<*>? = null

  override fun navigate(requestFocus: Boolean) {}
}