/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.r.psi.classes.r6.R6ClassInfo
import com.intellij.r.psi.classes.s4.classInfo.RS4ClassInfo
import com.intellij.r.psi.classes.s4.methods.RS4GenericOrMethodInfo
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.stubs.RCallExpressionStub

class RSkeletonCallExpressionStub(parent: StubElement<*>,
                                  elementType: RSkeletonCallExpressionElementType,
                                  override val s4ClassInfo: RS4ClassInfo)
  : StubBase<RCallExpression>(parent, elementType), RCallExpressionStub {
  override val s4GenericOrMethodInfo: RS4GenericOrMethodInfo? = null
  override val r6ClassInfo: R6ClassInfo? = null

  override fun toString(): String {
    return "RSkeletonCallExpressionStub:'${s4ClassInfo.className}'"
  }
}
