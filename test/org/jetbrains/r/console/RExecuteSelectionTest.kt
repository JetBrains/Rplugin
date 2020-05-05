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

  fun testSelectPrevExpressionSameLine() {
    doTest("print(123); 1 <caret>+\n 2 +\n 3 +\n4 + 5", "print(123); 1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testSelectPrevExpressionNextLine() {
    doTest("print(123); 1 +\n <caret>2 +\n 3 +\n4 + 5", "print(123); 1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testComment() {
    doTest("print(1); print(2) # <caret>comment", "print(1); print(2)")
  }

  fun testDontSelectPrevExpressionNewLine() {
    doTest("print(123)\n 1 <caret>+\n 2 +\n 3 +\n4 + 5", "1 +\n 2 +\n 3 +\n4 + 5")
  }

  fun testExecuteInIfSameLine() {
    doTest("if (f) 1 +<caret> 2", "if (f) 1 + 2")
  }

  fun testExecuteInIfNewLine() {
    doTest("if (f)\n 1 +<caret> 2", "1 + 2")
  }

  fun testExecuteIf() {
    doTest("if (<caret>f) 1 + 2", "if (f) 1 + 2")
  }

  fun testExecuteIfKeyword() {
    doTest("i<caret>f (f) 1 + 2", "if (f) 1 + 2")
  }

  fun testExecuteElseSameLine() {
    doTest("if (f) 1 + 2 else 3 <caret>+ 4", "if (f) 1 + 2 else 3 + 4")
  }

  fun testExecuteElseNewLine() {
    doTest("if (f)\n 1 + 2\nelse\n 3 <caret>+ 4", "3 + 4")
  }

  fun testExecuteIfKeywordElse() {
    doTest("if (f) 1 + 2\nel<caret>se 3 + 4", "if (f) 1 + 2\nelse 3 + 4")
  }

  fun testExecuteInBlock() {
    doTest("{ print(123)\nprint(32<caret>1)\n print(3424) }", "print(321)")
  }

  fun testExecuteEmptyLineInBlock() {
    doTest("{ print(123)\n<caret>\n print(321)\n print(3424) }", "print(321)")
  }

  fun textExecuteTopLevel() {
    doTest("print(123)\nprint(32<caret>1)\n print(3424)", "print(321)")
  }

  fun testExecuteRepeatSameLine() {
    doTest("repeat pri<caret>nt(1)", "repeat print(1)")
  }
  fun testExecuteRepeatNewLine() {
    doTest("repeat\n  pri<caret>nt(1)", "print(1)")
  }

  fun testWhileSameLine() {
    doTest("while (x < 10) x <-<caret> x + 1", "while (x < 10) x <- x + 1")
  }

  fun testWhileNewLine() {
    doTest("while (x < 10)\n  x <-<caret> x + 1", "x <- x + 1")
  }

  fun testWhileKeyword() {
    doTest("while (x<caret> < 10) x <- x + 1", "while (x < 10) x <- x + 1")
  }

  fun testForSameLine() {
    doTest("for (i in 1:10) prin<caret>t(i)", "for (i in 1:10) print(i)")
  }

  fun testForNewLine() {
    doTest("for (i in 1:10)\n  prin<caret>t(i)", "print(i)")
  }

  fun testForKeyword() {
    doTest("f<caret>or (i in 1:10) print(i)", "for (i in 1:10) print(i)")
  }

  fun testFunctionSameLine() {
    doTest("function(x) pri<caret>nt(1)", "function(x) print(1)")
  }

  fun testFunctionNewLine() {
    doTest("function(x)\n pri<caret>nt(1)", "print(1)")
  }

  fun testFuctionKeyword() {
    doTest("functi<caret>on(x) print(1)", "function(x) print(1)")
  }

  fun testNewline() {
    doTest("1 + 2 + 3<caret>\n", "1 + 2 + 3")
  }

  fun testBlockStart() {
    doTest("{<caret>\n print(1)\n print(2)\n print(3)\n}","{\n print(1)\n print(2)\n print(3)\n}")
  }

  fun testBlockEnd() {
    doTest("{\n print(1)\n print(2)\n print(3)\n<caret>}","{\n print(1)\n print(2)\n print(3)\n}")
  }

  fun testBlockStartWithExpression() {
    doTest("{ print(1)<caret>\n print(2)\n print(3)\n}","{ print(1)\n print(2)\n print(3)\n}")
  }

  fun testNestedBlock() {
    doTest("{\n { print(1)<caret>\n print(2)\n print(3)\n}\n print(123) }","{ print(1)\n print(2)\n print(3)\n}")
  }

  fun testIf() {
    doTest("if(T) { print(1)<caret>\n print(2)\n print(3)\n}","if(T) { print(1)\n print(2)\n print(3)\n}")
  }

  fun testInsideIf() {
    doTest("if(T) { print(1)\n print(2)<caret>\n print(3)\n}","print(2)")
  }

  // Discussable behaviour

  fun testSelectNextExpression() {
    doTest("1 +\n <caret>2 +\n 3 +\n4 + 5; print(123)", "1 +\n 2 +\n 3 +\n4 + 5; print(123)")
  }

  fun testSelectNextExpressionAfterBody() {
    doTest("if (f)\n <caret>10; print(123)", "10; print(123)")
  }

  fun testSelectNextExpressionInTheEndOfBlock() {
    // Note: will be syntax error during execution!
    doTest("{\n 10\n 20<caret> }; print(123)", "20 }; print(123)")
  }


  private fun doTest(text: String, expectedSelection: String) {
    myFixture.configureByText("Foo.R", text)
    TestCase.assertEquals(expectedSelection, REditorActionUtil.getSelectedCode(myFixture.editor)?.code)
  }
}