/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfo
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
    val s4Info = stub.s4ClassInfo?.getDeclarationText(project)
    //todo reconsider impl
    val r6Info = stub.r6ClassInfo.toString()

    return s4Info ?: r6Info
  }

  override fun getArgumentList(): RArgumentList {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun getAssociatedS4ClassInfo(): RS4ClassInfo? = myStub.s4ClassInfo

  override fun getAssociatedR6ClassInfo(): R6ClassInfo? = myStub.r6ClassInfo

  override fun getReference(): RReferenceBase<*>? = null

  override fun navigate(requestFocus: Boolean) {}
}