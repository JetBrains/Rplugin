/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.skeleton.psi

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.r.psi.parsing.RParserDefinition
import com.intellij.r.psi.psi.api.RFile

class RSkeletonFileStub : PsiFileStubImpl<RFile>(null) {
  override fun getType(): IStubFileElementType<*> = RParserDefinition.FILE
}