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
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.classes.s4.context.setClass.RS4SlotDeclarationContext
import org.jetbrains.r.lexer.RLexer
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

class RFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner(): WordsScanner {
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
      getAssignmentType(element)?.let { return it }
    }
    val parent = element.parent
    if (parent is RAssignmentStatement) {
      getAssignmentType(parent)?.let { return it }
    }

    if (element is RParameter || parent is RParameter && parent.variable == element) {
      return RBundle.message("find.usages.parameter")
    }

    if (element is RStringLiteralExpression &&
        RS4ContextProvider.getS4Context(element, RS4SetClassClassNameContext::class) != null) {
      return RBundle.message("find.usages.s4.class")
    }

    if (RPsiUtil.getNamedArgumentByNameIdentifier(element as RPsiElement) != null &&
        RS4ContextProvider.getS4Context(element, RS4SlotDeclarationContext::class) != null) {
      return RBundle.message("find.usages.s4.slot")
    }

    return RBundle.message("find.usages.variable")
  }

  private fun getAssignmentType(assignment: RAssignmentStatement): String? {
    if (assignment is RSkeletonAssignmentStatement) {
      return when (assignment.stub.type) {
        LibrarySummary.RLibrarySymbol.Type.FUNCTION -> RBundle.message("find.usages.function")
        LibrarySummary.RLibrarySymbol.Type.S4GENERIC -> RBundle.message("find.usages.s4.generic")
        LibrarySummary.RLibrarySymbol.Type.S4METHOD -> RBundle.message("find.usages.s4.method")
        LibrarySummary.RLibrarySymbol.Type.DATASET -> RBundle.message("find.usages.dataset")
        else -> null
      }
    }
    val assignedValue = assignment.assignedValue
    return if (assignedValue is RFunctionExpression) RBundle.message("find.usages.function")
    else null
  }

  override fun getDescriptiveName(element: PsiElement): String {
    if (element is RAssignmentStatement) {
      return element.assignee?.text ?: element.name
    }

    if (element is RStringLiteralExpression) {
      return element.name ?: element.text
    }

    // this will be used e.g. when renaming function parameters
    return element.text
  }


  override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
    return when (element) {
      is RStringLiteralExpression -> element.name ?: element.text
      else -> element.text
    }
  }
}
