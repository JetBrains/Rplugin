/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
package org.jetbrains.r.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.r.parsing.RElementTypes

private val bracePairs = arrayOf(
  BracePair(RElementTypes.R_LPAR, RElementTypes.R_RPAR, false),
  BracePair(RElementTypes.R_LBRACKET, RElementTypes.R_RBRACKET, false),
  BracePair(RElementTypes.R_LDBRACKET, RElementTypes.R_RDBRACKET, false),
  BracePair(RElementTypes.R_LBRACE, RElementTypes.R_RBRACE, false))

class RBraceMatcher : PairedBraceMatcher {
  override fun getPairs(): Array<BracePair> = bracePairs

  override fun getPairs(): Array<BracePair> = pairs

  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

  override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
