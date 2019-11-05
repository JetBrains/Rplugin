/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.actions.REditorActionUtil

class RExecuteSelectionTest : RUsefulTestCase() {

  fun testSelectExpressionBegin() {
    doTest("1 +<caret> 2 + 3 + 4", "1 + 2 + 3 + 4")
  }

  fun testSelectExpressionMiddle() {
    doTest("1 + 22<caret>2 + 3 + 4", "1 + 222 + 3 + 4")
  }

  fun testSelectExpressionEnd() {
    doTest("1 + 2 + 3 + <caret>4", "1 + 2 + 3 + 4")
  }

  fun testSelectMultiLineBegin() {
    doTest("1 <caret>+\n 2 +\n 3 +\n4 + 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testSelectMultiLineMiddle() {
    doTest("1 +\n 2 +\n 3<caret> +\n4 + 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testSelectMultiLineEnd() {
    doTest("1 +\n 2 +\n 3 +\n4 +<caret> 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testSelectMultiLineAssignment() {
    doTest("x <- 1 <caret>+\n 2 +\n 3 +\n4 + 5", "x <- 1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testDontSelectPrevExpression() {
    doTest("print(123); 1 <caret>+\n 2 +\n 3 +\n4 + 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testDontSelectPrevExpressionNewLine() {
    doTest("print(123)\n 1 <caret>+\n 2 +\n 3 +\n4 + 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testExecuteInIf() {
    doTest("if (f) 1 +<caret> 2", "1 + 2")
  }

  fun testExecuteIf() {
    doTest("if (<caret>f) 1 + 2", "if (f) 1 + 2")
  }

  fun testExecuteIfKeyword() {
    doTest("i<caret>f (f) 1 + 2", "if (f) 1 + 2")
  }

  fun testExecuteElse() {
    doTest("if (f) 1 + 2 else 3 <caret>+ 4", "3 + 4")
  }

  fun testExecuteIfKeywordElse() {
    doTest("if (f) 1 + 2 el<caret>se 3 + 4", "if (f) 1 + 2 else 3 + 4")
  }

  fun testExecuteInBlock() {
    doTest("{ print(123)\nprint(32<caret>1)\n print(3424) }", "print(321)")
  }

  fun textExecuteTopLevel() {
    doTest("print(123)\nprint(32<caret>1)\n print(3424)", "print(321)")
  }

  fun testExecuteRepeat() {
    doTest("repeat pri<caret>nt(1)", "print(1)")
  }

  fun testExecuteRepeatKeyword() {
    doTest("repe<caret>at print(1)", "repeat print(1)")
  }

  fun testWhile() {
    doTest("while (x < 10) x <-<caret> x + 1", "x <- x + 1")
  }

  fun testWhileKeyword() {
    doTest("while (x<caret> < 10) x <- x + 1", "while (x < 10) x <- x + 1")
  }

  fun testFor() {
    doTest("for (i in 1:10) prin<caret>t(i)", "print(i)")
  }

  fun testForKeyword() {
    doTest("f<caret>or (i in 1:10) print(i)", "for (i in 1:10) print(i)")
  }

  fun testFunction() {
    doTest("function(x) pri<caret>nt(1)", "print(1)")
  }

  fun testFuctionKeyword() {
    doTest("functi<caret>on(x) print(1)", "function(x) print(1)")
  }

  fun testNewline() {
    doTest("1 + 2 + 3<caret>\n", "1 + 2 + 3")
  }

  fun testBlock() {
    doTest("{ print(1)\n<caret> print(2)\n print(3)\n}","{ print(1)\n print(2)\n print(3)\n}")
  }

  fun testNestedBlock() {
    doTest("{ { print(1)\n<caret> print(2)\n print(3)\n}\n print(123) }","{ print(1)\n print(2)\n print(3)\n}")
  }

  fun testIf() {
    doTest("if { print(1)\n<caret> print(2)\n print(3)\n}","if { print(1)\n print(2)\n print(3)\n}")
  }

  private fun doTest(text: String, expectedSelection: String) {
    myFixture.configureByText("Foo.R", text)
    TestCase.assertEquals(expectedSelection, REditorActionUtil.getSelectedCode(myFixture.editor)?.code)
  }
}