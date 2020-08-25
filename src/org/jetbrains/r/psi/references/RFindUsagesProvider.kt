// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.r.RBundle
import org.jetbrains.r.lexer.RLexer
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RFunctionExpression
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RParameter

class RFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner(): WordsScanner? {
    return DefaultWordsScanner(RLexer(), TokenSet.create(RElementTypes.R_IDENTIFIER),
                               TokenSet.create(RParserDefinition.END_OF_LINE_COMMENT),
                               TokenSet.create(RElementTypes.R_STRING_LITERAL_EXPRESSION))
  }


  override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
    //        isLibraryFile(psiElement.getContainingFile())
    return psiElement is PsiNamedElement || psiElement is RIdentifierExpression
  }


  override fun getHelpId(psiElement: PsiElement): String? {
    return null
  }


  override fun getType(element: PsiElement): String {
    if (element is RAssignmentStatement) {
      val assignedValue = element.assignedValue
      if (assignedValue is RFunctionExpression) {
        return RBundle.message("find.usages.function")
      }
    }

    return if (element is RParameter) RBundle.message("find.usages.parameter") else RBundle.message("find.usages.variable")

  }


  override fun getDescriptiveName(element: PsiElement): String {
    if (element is RAssignmentStatement) {
      return element.assignee?.text ?: element.name
    }

    // this will be used e.g. when renaming function parameters
    return element.text
  }


  override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
    return element.text
  }
}
