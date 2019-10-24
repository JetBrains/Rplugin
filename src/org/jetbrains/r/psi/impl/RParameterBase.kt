/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.impl

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.IStubElementType
import org.jetbrains.r.psi.RBaseElementImpl
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.stubs.RParameterStub

abstract class RParameterBase : RBaseElementImpl<RParameterStub>, RExpression, PsiNameIdentifierOwner {
  internal constructor(node: com.intellij.lang.ASTNode) : super(node)
  internal constructor(stub: RParameterStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}
