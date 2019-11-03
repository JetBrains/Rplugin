/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parsing

import com.intellij.lang.DefaultASTFactoryImpl
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.r.parsing.RElementTypes.R_NL

class RAstFactory : DefaultASTFactoryImpl() {
  override fun createLeaf(type: IElementType, text: CharSequence): LeafElement {
    if (type == R_NL) {
      return RNextLinePsiWhiteSpace(type, text)
    }
    return super.createLeaf(type, text)
  }
}

private class RNextLinePsiWhiteSpace(type: IElementType, text: CharSequence) : LeafPsiElement(type, text), PsiWhiteSpace
