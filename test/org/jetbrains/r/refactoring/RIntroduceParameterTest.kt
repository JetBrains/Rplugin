/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import com.intellij.refactoring.util.CommonRefactoringUtil
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.refactoring.RIntroduceLocalHandler
import org.jetbrains.r.refactoring.RIntroduceParameterHandler

class RIntroduceParameterTest : RUsefulTestCase() {
  fun testOneExpression() {
    doTest(
      "f <- function() return(1 + <selection>2 * 3</selection>)",
      "f <- function(value = 2 * 3) return(1 + <caret>value)"
    )
  }

  fun testWithParams() {
    doTest(
      "f <- function(x, y) return(x + <selection>2 * y</selection>)",
      "f <- function(x, y, value = 2 * y) return(x + <caret>value)"
    )
  }

  fun testTwoOccurrences() {
    doTest(
      """
        |f <- function(x, y) {
        |  if (x > y) {
        |    return(<selection>x + y / 2</selection> + 1)
        |  } else {
        |    z = x + y / 2
        |    return(z + x)
        |  }
        |}
      """.trimMargin(),
      """
        |f <- function(x, y, value = x + y / 2) {
        |  if (x > y) {
        |    return(value + 1)
        |  } else {
        |    z = value
        |    return(z + x)
        |  }
        |}
      """.trimMargin()
    )
  }

  fun testInDefaultValue() {
    doTest(
      "f <- function(x, y = x + <selection>2 * 3</selection>) return(x + y + 2 * 3)",
      "f <- function(x, y = x + <caret>value, value = 2 * 3) return(x + y + value)"
    )
  }

  fun testNoopGlobalScope() = doTestNoop("print(1 + <selection>2 * 3</selection>)")

  fun testNoopParameter() = doTestNoop("f <- function(x, <selection>y</selection>, z) x + y + z")
  fun testNoopAssignee() = doTestNoop("f <- function(a) <selection>a[2]</selection> <- 20")
  fun testNoopVariable() = doTestNoop("f <- function(a, b) print(<selection>a</selection> + b)")
  fun testNoopReturn() = doTestNoop("f <- function(x) <selection>return(x + 1)</selection>")
  fun testNoopWhile() = doTestNoop("f <- function(x) <selection>while (x < 10) { x = g() }</selection>")
  fun testNoopBreak() = doTestNoop("f <- function(x) while (true) if (g()) <selection>break</selection>")

  private fun doTest(text: String, expected: String, replaceAll: Boolean = true) {
    myFixture.configureByText("a.R", text)
    myFixture.editor.settings.isVariableInplaceRenameEnabled = false

    val handler = RIntroduceParameterHandler()
    val operation = RIntroduceLocalHandler.Companion.IntroduceOperation(project, myFixture.editor, myFixture.file)
    operation.replaceAll = replaceAll
    handler.invokeOperation(operation)

    myFixture.checkResult(expected)
  }

  private fun doTestNoop(text: String) {
    myFixture.configureByText("a.R", text)
    val handler = RIntroduceParameterHandler()
    myFixture.editor.settings.isVariableInplaceRenameEnabled = false
    try {
      handler.invoke(project, myFixture.editor, myFixture.file, null)
      TestCase.fail()
    } catch (ignored: CommonRefactoringUtil.RefactoringErrorHintException) {
    }
  }
}
