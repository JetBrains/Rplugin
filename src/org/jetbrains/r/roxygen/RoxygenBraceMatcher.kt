/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.*

class RoxygenBraceMatcher : PairedBraceMatcher {
  override fun getPairs(): Array<BracePair> = arrayOf(
    BracePair(ROXYGEN_LPAR, ROXYGEN_RPAR, false),
    BracePair(ROXYGEN_LBRACKET, ROXYGEN_RBRACKET, false),
    BracePair(ROXYGEN_LANGLE, ROXYGEN_RANGLE, false))

  /**
   * Because of the peculiarities of the Roxygen Lexer, [RoxygenTypedHandler] is engaged in insertion
   */
  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = false

  override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
