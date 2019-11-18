/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import org.jetbrains.r.psi.api.RPsiElement

class RExtendWordSelectionHandler : ExtendWordSelectionHandlerBase() {
  override fun canSelect(e: PsiElement): Boolean {
    return e is RPsiElement
  }

  /** Disable basic selection handler for R psi elements */
  class RBasicFilter : Condition<PsiElement> {
    override fun value(element: PsiElement): Boolean {
      if (element is RPsiElement || element.parent is RPsiElement) {
        return false
      }
      return true
    }
  }
}
