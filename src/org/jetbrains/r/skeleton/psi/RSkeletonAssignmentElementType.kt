/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.StringRef
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.stubs.RAssignmentCompletionIndex
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex
import org.jetbrains.r.psi.stubs.RInternalAssignmentCompletionIndex
import org.jetbrains.r.psi.stubs.RStubElementType

class RSkeletonAssignmentElementType : RStubElementType<RSkeletonAssignmentStub, RAssignmentStatement>("R bin assignment") {
  override fun createPsi(stub: RSkeletonAssignmentStub): RAssignmentStatement {
    return RSkeletonAssignmentStatement(stub)
  }

  override fun serialize(stub: RSkeletonAssignmentStub, dataStream: StubOutputStream) {
    dataStream.writeName(stub.name)
    dataStream.writeInt(stub.type.number)
    if (stub.isFunctionDeclaration) {
      dataStream.writeName(stub.parameters)
    }
    dataStream.writeBoolean(stub.exported)
    stub.extraNamedArguments.serialize(dataStream)
  }

  override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): RSkeletonAssignmentStub {
    val name = StringRef.toString(dataStream.readName())
    val typeNumber = dataStream.readInt()
    val type: RSkeletonSymbolType = RSkeletonSymbolType.forNumber(typeNumber) ?:
                                    throw IllegalStateException("Unknown type number $typeNumber")
    val parameters = if (type == RSkeletonSymbolType.FUNCTION) StringRef.toString(dataStream.readName()) else ""
    val exported = dataStream.readBoolean()
    val extraNamedArguments = RExtraNamedArgumentsInfo.deserialize(dataStream)
    return RSkeletonAssignmentStub(parentStub, this, name, type, parameters, exported, extraNamedArguments)
  }

  override fun createStub(psi: RAssignmentStatement, parentStub: StubElement<*>?): RSkeletonAssignmentStub {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun createElement(node: ASTNode): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun indexStub(stub: RSkeletonAssignmentStub, sink: IndexSink) {
    val name = stub.name
    RAssignmentNameIndex.sink(sink, name)
    if (stub.exported) {
      RAssignmentCompletionIndex.sink(sink, "")
    }
    if (stub.type != RSkeletonSymbolType.DATASET) {
      // data sets cannot be accessed by `:::` operator
      RInternalAssignmentCompletionIndex.sink(sink, "")
    }
  }
}