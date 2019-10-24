/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.bin.psi

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.psi.api.RFile

class RBinFileStub : PsiFileStubImpl<RFile>(null) {
  override fun getType(): IStubFileElementType<*> = RParserDefinition.FILE
}