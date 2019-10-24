/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import com.intellij.refactoring.util.CommonRefactoringUtil
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.refactoring.RIntroduceLocalHandler
import org.jetbrains.r.refactoring.RIntroduceVariableHandler

class RIntroduceVariableTest : RUsefulTestCase() {
  fun testOneExpression() {
    doTest(
      "print(a + <selection>b * c</selection>)",
      """
        value <- b * c
        print(a + <caret>value)
      """.trimIndent()
    )
  }

  fun testTwo() {
    doTest(
      "print(a + <selection>b * c</selection> / (a - b * c))",
      """
        value <- b * c
        print(a + <caret>value / (a - value))
      """.trimIndent()
    )
  }

  fun testWholeExpression() {
    doTest(
      "<selection>a + b * c</selection>",
      "value <- a + b * c"
    )
  }

  fun testWholeExpression2() {
    doTest(
      """
        <selection>a + b</selection>
        print(a + b + c)
      """.trimIndent(),
      """
        <caret>value <- a + b
        print(value + c)
      """.trimIndent()
    )
  }

  fun testAnchor1() {
    doTest(
      """
        |if (x) {
        |  print(<selection>a + b</selection> + 1)
        |  print(a + b + 2)
        |}
      """.trimMargin(),
      """
        |if (x) {
        |  value <- a + b
        |  print(value + 1)
        |  print(value + 2)
        |}
      """.trimMargin()
    )
  }

  fun testAnchor2() {
    doTest(
      """
        |if (x) {
        |  print(<selection>a + b</selection> + 1)
        |} else {
        |  print(a + b + 2)
        |}
      """.trimMargin(),
      """
        |value <- a + b
        |if (x) {
        |  print(value + 1)
        |} else {
        |  print(value + 2)
        |}
      """.trimMargin()
    )
  }

  fun testBracesIf() {
    doTest(
      """
        |if (x) print(<selection>a + b</selection>)
      """.trimMargin(),
      """
        |if (x) {
        |  value <- a + b
        |  print(<caret>value)
        |}
      """.trimMargin()
    )
  }

  fun testBracesIfWhole() {
    doTest(
      """
        |if (x) <selection>a + b</selection>
      """.trimMargin(),
      """
        |if (x) {
        |  <caret>value <- a + b
        |}
      """.trimMargin()
    )
  }

  fun testBracesFunction() {
    doTest(
      """
        |f <- function(a, b, c) <selection>a + b</selection> + c
      """.trimMargin(),
      """
        |f <- function(a, b, c) {
        |  value <- a + b
        |  return(<caret>value + c)
        |}
      """.trimMargin()
    )
  }

  fun testBracesFunctionWhole() {
    doTest(
      """
        |f <- function(a, b, c) <selection>a + b</selection>
      """.trimMargin(),
      """
        |f <- function(a, b, c) {
        |  value <- a + b
        |  return(<caret>value)
        |}
      """.trimMargin()
    )
  }

  fun testBracesFunctionReturn() {
    doTest(
      """
        |f <- function(a, b, c) return(<selection>a + b</selection>)
      """.trimMargin(),
      """
        |f <- function(a, b, c) {
        |  value <- a + b
        |  return(<caret>value)
        |}
      """.trimMargin()
    )
  }

  fun testNoopParameter() = doTestNoop("f <- function(x, <selection>y</selection>, z) x + y + z")
  fun testNoopAssignee() = doTestNoop("<selection>a[2]</selection> <- 20")
  fun testNoopVariable() = doTestNoop("print(<selection>a</selection> + b)")
  fun testNoopReturn() = doTestNoop("f <- function(x) <selection>return(x + 1)</selection>")
  fun testNoopWhile() = doTestNoop("<selection>while (x < 10) { x = f() }</selection>")
  fun testNoopBreak() = doTestNoop("while (true) if (f()) <selection>break</selection>")

  private fun doTest(text: String, expected: String, replaceAll: Boolean = true) {
    myFixture.configureByText("a.R", text)
    myFixture.editor.settings.isVariableInplaceRenameEnabled = false

    val handler = RIntroduceVariableHandler()
    val operation = RIntroduceLocalHandler.Companion.IntroduceOperation(project, myFixture.editor, myFixture.file)
    operation.replaceAll = replaceAll
    handler.invokeOperation(operation)

    myFixture.checkResult(expected)
  }

  private fun doTestNoop(text: String) {
    myFixture.configureByText("a.R", text)
    val handler = RIntroduceVariableHandler()
    myFixture.editor.settings.isVariableInplaceRenameEnabled = false
    try {
      handler.invoke(project, myFixture.editor, myFixture.file, null)
      TestCase.fail()
    } catch (ignored: CommonRefactoringUtil.RefactoringErrorHintException) {
    }
  }
}
