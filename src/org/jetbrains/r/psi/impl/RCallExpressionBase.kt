/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.jetbrains.r.psi.RBaseElementImpl
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.references.RReferenceBase
import org.jetbrains.r.psi.stubs.RCallExpressionStub

abstract class RCallExpressionBase : RBaseElementImpl<RCallExpressionStub>, RExpression {
  constructor(node: ASTNode) : super(node)
  constructor(stub: RCallExpressionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): RReferenceBase<*>? = null
}