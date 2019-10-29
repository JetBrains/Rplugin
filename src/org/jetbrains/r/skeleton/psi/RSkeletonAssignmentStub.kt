/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.stubs.RAssignmentStub

class RSkeletonAssignmentStub(parent: StubElement<*>,
                              elementType: RSkeletonAssignmentElementType,
                              private val myName: String,
                              private val isFunc: Boolean,
                              val parameters: String
) : StubBase<RAssignmentStatement>(parent, elementType), RAssignmentStub {
  override fun isRight(): Boolean = true

  override fun isTopLevelAssignment(): Boolean = true

  override fun getName(): String = myName

  override fun isFunctionDeclaration(): Boolean = isFunc
}