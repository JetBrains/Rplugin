/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RParameter
import org.jetbrains.r.refactoring.RNamesValidator


class RRenameInputValidator : RenameInputValidator {
  override fun getPattern(): ElementPattern<PsiElement> {
    return StandardPatterns.or(
      PlatformPatterns.psiElement(RAssignmentStatement::class.java),
      PlatformPatterns.psiElement(RParameter::class.java),
      PlatformPatterns.psiElement(RIdentifierExpression::class.java)
    )
  }

  override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
    return if (element is RAssignmentStatement) {
      val name = element.name
      !RNamesValidator.isOperatorIdentifier(name) || RNamesValidator.isOperatorIdentifier(newName)
    }
    else true
  }
}