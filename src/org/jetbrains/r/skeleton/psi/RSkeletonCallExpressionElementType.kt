/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.stubs.RStubElementType
import org.jetbrains.r.psi.stubs.classes.RS4ClassNameIndex

class RSkeletonCallExpressionElementType : RStubElementType<RSkeletonCallExpressionStub, RCallExpression>("R bin s4 r6") {
  override fun createPsi(stub: RSkeletonCallExpressionStub): RCallExpression {
    return RSkeletonCallExpression(stub)
  }

  override fun serialize(stub: RSkeletonCallExpressionStub, dataStream: StubOutputStream) {
    stub.s4ClassInfo.serialize(dataStream)
  }

  override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): RSkeletonCallExpressionStub {
    val s4ClassInfo = RS4ClassInfo.deserialize(dataStream)
    return RSkeletonCallExpressionStub(parentStub, this, s4ClassInfo)
  }

  override fun createStub(psi: RCallExpression, parentStub: StubElement<*>?): RSkeletonCallExpressionStub {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun createElement(node: ASTNode): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun indexStub(stub: RSkeletonCallExpressionStub, sink: IndexSink) {
    val name = stub.s4ClassInfo.className
    RS4ClassNameIndex.sink(sink, name)
  }
}