/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton.psi

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.stubs.RCallExpressionStub

class RSkeletonCallExpressionStub(parent: StubElement<*>,
                              elementType: RSkeletonCallExpressionElementType,
                              override val s4ClassInfo: RS4ClassInfo)
  : StubBase<RCallExpression>(parent, elementType), RCallExpressionStub
