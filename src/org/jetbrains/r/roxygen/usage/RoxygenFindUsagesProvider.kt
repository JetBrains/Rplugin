/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.usage

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.r.RBundle
import org.jetbrains.r.roxygen.lexer.RoxygenLexer
import org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.ROXYGEN_IDENTIFIER
import org.jetbrains.r.roxygen.parsing.RoxygenParserDefinition
import org.jetbrains.r.roxygen.psi.api.RoxygenParameter

class RoxygenFindUsagesProvider : FindUsagesProvider {

  override fun getWordsScanner(): WordsScanner? {
    return DefaultWordsScanner(RoxygenLexer(),
                               TokenSet.create(ROXYGEN_IDENTIFIER),
                               TokenSet.EMPTY,
                               RoxygenParserDefinition.STRINGS)
  }

  override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is PsiNamedElement

  override fun getHelpId(psiElement: PsiElement): String? = null

  override fun getType(element: PsiElement): String {
    return if (element is RoxygenParameter) {
      RBundle.message("roxygen.parameter")
    } else {
      RBundle.message("roxygen.help.page.link")
    }
  }

  override fun getDescriptiveName(element: PsiElement): String = element.text

  override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text
}