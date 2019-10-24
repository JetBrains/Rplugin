/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.bin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.StringRef
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.stubs.RAssignmentCompletionIndex
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex
import org.jetbrains.r.psi.stubs.RStubElementType

class RBinAssignmentElementType : RStubElementType<RBinAssignmentStub, RAssignmentStatement>("R bin assignment") {
  override fun createPsi(stub: RBinAssignmentStub): RAssignmentStatement {
    return RBinAssignmentStatement(stub)
  }

  override fun serialize(stub: RBinAssignmentStub, dataStream: StubOutputStream) {
    dataStream.writeName(stub.name)
    dataStream.writeBoolean(stub.isFunctionDeclaration)
    if (stub.isFunctionDeclaration) {
      dataStream.writeName(stub.parameters)
    }
  }

  override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): RBinAssignmentStub {
    val name = StringRef.toString(dataStream.readName())
    val isFunctionDefinition = dataStream.readBoolean()
    val parameters = if (isFunctionDefinition) StringRef.toString(dataStream.readName()) else ""
    return RBinAssignmentStub(parentStub, this, name, isFunctionDefinition, parameters)
  }

  override fun createStub(psi: RAssignmentStatement, parentStub: StubElement<*>?): RBinAssignmentStub {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun createElement(node: ASTNode): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun indexStub(stub: RBinAssignmentStub, sink: IndexSink) {
    val name = stub.name
    RAssignmentNameIndex.sink(sink, name)
    RAssignmentCompletionIndex.sink(sink, "")
  }
}