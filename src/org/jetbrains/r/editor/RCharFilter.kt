/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.r.psi.RLanguage


class RCharFilter : CharFilter() {
  override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
    if (lookup?.psiElement?.language !is RLanguage) return null

    if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
      return Result.ADD_TO_PREFIX
    }
    return Result.HIDE_LOOKUP
  }
}
