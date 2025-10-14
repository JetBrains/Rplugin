/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.r.psi.classes.s4.methods.RS4GenericOrMethodInfo
import com.intellij.r.psi.hints.parameterInfo.RExtraNamedArgumentsInfo
import com.intellij.r.psi.packages.LibrarySummary
import com.intellij.r.psi.packages.isFunctionDeclaration
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.stubs.RAssignmentStub

typealias RSkeletonSymbolType = LibrarySummary.RLibrarySymbol.Type

class RSkeletonAssignmentStub(parent: StubElement<*>,
                              elementType: RSkeletonAssignmentElementType,
                              private val myName: String,
                              val type: RSkeletonSymbolType,
                              val parameters: String,
                              val exported: Boolean,
                              val extraNamedArguments: RExtraNamedArgumentsInfo,
                              val s4GenericOrMethodInfo: RS4GenericOrMethodInfo?
) : StubBase<RAssignmentStatement>(parent, elementType), RAssignmentStub {
  override fun isRight(): Boolean = true

  override fun isTopLevelAssignment(): Boolean = true

  override fun getName(): String = myName

  override fun isFunctionDeclaration(): Boolean = type.isFunctionDeclaration

  override fun isPrimitiveFunctionDeclaration(): Boolean = type == RSkeletonSymbolType.PRIMITIVE
}