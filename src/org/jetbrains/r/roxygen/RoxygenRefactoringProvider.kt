/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.jetbrains.r.roxygen.psi.api.RoxygenIdentifierExpression
import org.jetbrains.r.roxygen.psi.api.RoxygenParameter

class RoxygenRefactoringProvider : RefactoringSupportProvider() {
  override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
    return element is RoxygenParameter || element is RoxygenIdentifierExpression
  }
}