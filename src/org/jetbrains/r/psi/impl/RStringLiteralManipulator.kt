/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RStringLiteralManipulator : AbstractElementManipulator<RStringLiteralExpression>() {

  @Throws(IncorrectOperationException::class)
  override fun handleContentChange(element: RStringLiteralExpression, range: TextRange, newContent: String): RStringLiteralExpression {
    return element.setName(newContent) as RStringLiteralExpression
  }

  override fun getRangeInElement(element: RStringLiteralExpression): TextRange {
    val text = element.text
    return if (text[0].lowercaseChar() == 'r') {
      var dashCnt = 0
      while (text[2 + dashCnt] == '-') ++dashCnt
      TextRange(dashCnt + 3, element.textRange.length - 2 - dashCnt)
    }
    else TextRange(1, element.textRange.length - 1)
  }
}
