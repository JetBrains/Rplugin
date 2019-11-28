/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.misc

import com.intellij.openapi.actionSystem.IdeActions
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase

class SelectionTest : RUsefulTestCase() {

  fun testIdentifiersWithDot() {
    doTest("""
      foo()
      aaa.bb<caret>b.ccc <- 10
    """, """
      foo()
      <selection>aaa.bbb.ccc</selection> <- 10
    """)
  }

  fun testExtendExpression() {
    doTest("""
      foo()
      <selection><caret>aaa.bbb.ccc</selection> <- 10
      foo()
    """, """
      foo()
      <selection>aaa.bbb.ccc <- 10</selection>
      foo()
    """)
  }

  fun testSelectFunctionKeyword() {
    doTest("""
      func<caret>tion(x) x + 1
    """, """
      <selection>function</selection>(x) x + 1
    """)
  }

  private fun doTest(@Language("R") text: String, @Language("R") expected: String) {
    myFixture.configureByText("a.R", text.trimIndent())
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
    myFixture.checkResult(expected.trimIndent(), false)
  }
}