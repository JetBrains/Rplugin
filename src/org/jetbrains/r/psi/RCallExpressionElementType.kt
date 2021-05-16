/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfo
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfoUtil
import org.jetbrains.r.classes.s4.methods.RS4GenericInfo
import org.jetbrains.r.classes.s4.methods.RS4GenericOrMethodInfo
import org.jetbrains.r.classes.s4.methods.RS4MethodInfo
import org.jetbrains.r.classes.s4.methods.RS4MethodsUtil
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.impl.RCallExpressionImpl
import org.jetbrains.r.psi.stubs.RCallExpressionStub
import org.jetbrains.r.psi.stubs.RCallExpressionStubImpl
import org.jetbrains.r.psi.stubs.RStubElementType
import org.jetbrains.r.psi.stubs.classes.R6ClassNameIndex
import org.jetbrains.r.psi.stubs.classes.RS4ClassNameIndex
import org.jetbrains.r.psi.stubs.classes.RS4GenericIndex
import org.jetbrains.r.psi.stubs.classes.RS4MethodsIndex
import java.io.IOException

class RCallExpressionElementType(debugName: String) : RStubElementType<RCallExpressionStub, RCallExpression>(debugName) {
  override fun createElement(node: ASTNode): PsiElement = RCallExpressionImpl(node)

  override fun createPsi(stub: RCallExpressionStub): RCallExpression = RCallExpressionImpl(stub, this)

  override fun createStub(psi: RCallExpression, parentStub: StubElement<*>): RCallExpressionStub {
    return RCallExpressionStubImpl(parentStub, this, RS4ClassInfoUtil.parseS4ClassInfo(psi), RS4MethodsUtil.parseS4GenericOrMethodInfo(psi), R6ClassInfoUtil.parseR6ClassInfo(psi))
  }

  @Throws(IOException::class)
  override fun serialize(stub: RCallExpressionStub, dataStream: StubOutputStream) {
    dataStream.writeBoolean(stub.s4ClassInfo != null)
    stub.s4ClassInfo?.serialize(dataStream)
    dataStream.writeBoolean(stub.s4GenericOrMethodInfo != null)
    stub.s4GenericOrMethodInfo?.serialize(dataStream)

    dataStream.writeBoolean(stub.r6ClassInfo != null)
    stub.r6ClassInfo?.serialize(dataStream)
  }

  @Throws(IOException::class)
  override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): RCallExpressionStub {
    val s4ClassExists = dataStream.readBoolean()
    val s4ClassInfo = if (s4ClassExists) RS4ClassInfo.deserialize(dataStream) else null
    val s4GenericOtMethodExists = dataStream.readBoolean()
    val s4GenericOtMethodInfo = if (s4GenericOtMethodExists) RS4GenericOrMethodInfo.deserialize(dataStream) else null

    val r6ClassExists = dataStream.readBoolean()
    val r6ClassInfo = if (r6ClassExists) R6ClassInfo.deserialize(dataStream) else null
    return RCallExpressionStubImpl(parentStub, this, s4ClassInfo, s4GenericOtMethodInfo, r6ClassInfo)
  }

  override fun indexStub(stub: RCallExpressionStub, sink: IndexSink) {
    stub.s4ClassInfo?.className?.let { RS4ClassNameIndex.sink(sink, it) }
    stub.s4GenericOrMethodInfo?.let {
      when (it) {
        is RS4GenericInfo -> RS4GenericIndex.sink(sink, it.methodName)
        is RS4MethodInfo -> RS4MethodsIndex.sink(sink, it.methodName)
      }
    }
    stub.r6ClassInfo?.className?.let { R6ClassNameIndex.sink(sink, it) }
  }

  override fun shouldCreateStub(node: ASTNode?): Boolean {
    val call = node?.psi as? RCallExpression ?: return false
    return call.isFunctionFromLibrarySoft("setClass", "methods") ||
           call.isFunctionFromLibrarySoft("setGeneric", "methods") ||
           call.isFunctionFromLibrarySoft("setMethod", "methods") ||
           call.isFunctionFromLibrarySoft(R6ClassInfoUtil.R6CreateClassMethod, R6ClassInfoUtil.R6PackageName)
  }
}