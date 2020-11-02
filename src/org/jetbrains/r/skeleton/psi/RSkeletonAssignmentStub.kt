/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.stubs.RAssignmentStub

typealias RSkeletonSymbolType = LibrarySummary.RLibrarySymbol.Type

class RSkeletonAssignmentStub(parent: StubElement<*>,
                              elementType: RSkeletonAssignmentElementType,
                              private val myName: String,
                              val type: RSkeletonSymbolType,
                              val parameters: String,
                              val exported: Boolean,
                              val extraNamedArguments: RExtraNamedArgumentsInfo
) : StubBase<RAssignmentStatement>(parent, elementType), RAssignmentStub {
  override fun isRight(): Boolean = true

  override fun isTopLevelAssignment(): Boolean = true

  override fun getName(): String = myName

  override fun isFunctionDeclaration(): Boolean = type == RSkeletonSymbolType.FUNCTION

  override fun isPrimitiveFunctionDeclaration(): Boolean = type == RSkeletonSymbolType.PRIMITIVE
}