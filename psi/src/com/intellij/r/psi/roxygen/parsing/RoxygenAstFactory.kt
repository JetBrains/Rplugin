/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.parsing

import com.intellij.lang.DefaultASTFactoryImpl
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_NL
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_WS

class RoxygenAstFactory : DefaultASTFactoryImpl() {
  override fun createLeaf(type: IElementType, text: CharSequence): LeafElement {
    if (type == ROXYGEN_NL || type == ROXYGEN_WS) {
      return RoxygenPsiWhiteSpace(type, text)
    }
    return super.createLeaf(type, text)
  }
}

private class RoxygenPsiWhiteSpace(type: IElementType, text: CharSequence) : LeafPsiElement(type, text), PsiWhiteSpace
