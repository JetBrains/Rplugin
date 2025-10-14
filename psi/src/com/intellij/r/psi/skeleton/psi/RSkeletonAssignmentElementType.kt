/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.r.psi.classes.s4.methods.RS4GenericOrMethodInfo
import com.intellij.r.psi.hints.parameterInfo.RExtraNamedArgumentsInfo
import com.intellij.r.psi.packages.LibrarySummary
import com.intellij.r.psi.packages.isFunctionDeclaration
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.stubs.RAssignmentCompletionIndex
import com.intellij.r.psi.psi.stubs.RAssignmentNameIndex
import com.intellij.r.psi.psi.stubs.RInternalAssignmentCompletionIndex
import com.intellij.r.psi.psi.stubs.RStubElementType
import com.intellij.r.psi.psi.stubs.classes.RS4GenericIndex
import com.intellij.r.psi.psi.stubs.classes.RS4MethodsIndex
import com.intellij.util.IncorrectOperationException
import com.intellij.util.io.StringRef

class RSkeletonAssignmentElementType : RStubElementType<RSkeletonAssignmentStub, RAssignmentStatement>("R bin assignment") {
  override fun createPsi(stub: RSkeletonAssignmentStub): RAssignmentStatement {
    return RSkeletonAssignmentStatement(stub)
  }

  override fun serialize(stub: RSkeletonAssignmentStub, dataStream: StubOutputStream) {
    dataStream.writeName(stub.name)
    dataStream.writeInt(stub.type.number)
    dataStream.writeBoolean(stub.exported)
    if (stub.isFunctionDeclaration) {
      dataStream.writeName(stub.parameters)
      stub.extraNamedArguments.serialize(dataStream)
      dataStream.writeBoolean(stub.s4GenericOrMethodInfo != null)
      stub.s4GenericOrMethodInfo?.serialize(dataStream)
    }
  }

  override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): RSkeletonAssignmentStub {
    val name = StringRef.toString(dataStream.readName())
    val typeNumber = dataStream.readInt()
    val type: RSkeletonSymbolType = RSkeletonSymbolType.forNumber(typeNumber) ?:
                                    throw IllegalStateException("Unknown type number $typeNumber")
    val exported = dataStream.readBoolean()
    val (parameters, extraNamedArguments, s4MethodArgumentTypes) =
      if (type.isFunctionDeclaration) {
        val parameters = StringRef.toString(dataStream.readName())
        val extraNamedArguments = RExtraNamedArgumentsInfo.deserialize(dataStream)
        val isS4GenericOrMethodInfoExists = dataStream.readBoolean()
        val s4GenericOrMethodInfo =
          if (isS4GenericOrMethodInfoExists) RS4GenericOrMethodInfo.deserialize(dataStream)
          else null
        Triple(parameters, extraNamedArguments, s4GenericOrMethodInfo)
      }
      else Triple("", RExtraNamedArgumentsInfo(emptyList(), emptyList()), null)
    return RSkeletonAssignmentStub(parentStub, this, name, type, parameters, exported, extraNamedArguments, s4MethodArgumentTypes)
  }

  override fun createStub(psi: RAssignmentStatement, parentStub: StubElement<*>?): RSkeletonAssignmentStub {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun createElement(node: ASTNode): PsiElement {
    throw IncorrectOperationException("Operation not supported in: $javaClass")
  }

  override fun indexStub(stub: RSkeletonAssignmentStub, sink: IndexSink) {
    val name = stub.name
    when (stub.type) {
      LibrarySummary.RLibrarySymbol.Type.S4GENERIC -> RS4GenericIndex.sink(sink, name)
      LibrarySummary.RLibrarySymbol.Type.S4METHOD -> RS4MethodsIndex.sink(sink, name)
      else -> {
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
  }
}