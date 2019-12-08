/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference

object RenameUtil {
  fun acceptReference(reference: PsiReference, elementToRename: PsiNamedElement?): Boolean {
    val refElement = reference.element as? PsiNamedElement ?: return false
    return refElement.name == elementToRename?.name
  }

  fun fixTextRange(range: TextRange, reference: PsiReference): TextRange {
    return fixTextRange(range, reference.element)
  }

  fun fixTextRange(range: TextRange, reference: PsiElement): TextRange {
    val text = reference.text
    if (text.startsWith('`') && text.endsWith('`')) {
      return range.shiftRight(1).grown(-2)
    }
    return range
  }
}