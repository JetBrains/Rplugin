/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import com.intellij.refactoring.util.CommonRefactoringUtil
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.refactoring.extractmethod.RExtractMethodHandler

class RExtractMethodTest : RUsefulTestCase() {
  fun testSimple() {
    doTest("""
      |f <- function() {
      |  print(1)
      |  <selection>print(2 + 3)
      |  print(4)</selection>
      |  print(5 + 6)
      |}
    """.trimMargin(), """
      |foo <- function() {
      |  print(2 + 3)
      |  print(4)
      |}
      |
      |f <- function() {
      |  print(1)
      |  foo()
      |  print(5 + 6)
      |}""".trimMargin())
  }

  fun testParams() {
    doTest("""
      |f <- function(x) {
      |  print(1)
      |  y <- x + 2
      |  <selection>print(x)
      |  print(x * y)</selection>
      |  print(4)
      |}
    """.trimMargin(), """
      |foo <- function(x, y) {
      |  print(x)
      |  print(x * y)
      |}
      |
      |f <- function(x) {
      |  print(1)
      |  y <- x + 2
      |  foo(x, y)
      |  print(4)
      |}""".trimMargin())
  }

  fun testValueUsed() {
    doTest("""
      |f <- function(a, b, c) {
      |  g(a + <selection>b * c</selection>)
      |}
    """.trimMargin(), """
      |foo <- function(b, c) b * c
      |
      |f <- function(a, b, c) {
      |  g(a + foo(b, c))
      |}
    |""".trimMargin())
  }

  fun testReturnValue() {
    doTest("""
      |f <- function(x, y) {
      |  z <- x + y
      |  <selection>a <- 10
      |  if (z > a) {
      |    return(z)
      |  } else {
      |    d <- x - 2
      |    return(d * z)
      |  }</selection>
      |}
    """.trimMargin(), """
      |foo <- function(x, z) {
      |  a <- 10
      |  if (z > a) {
      |    return(z)
      |  } else {
      |    d <- x - 2
      |    return(d * z)
      |  }
      |}
      |
      |f <- function(x, y) {
      |  z <- x + y
      |  return(foo(x, z))
      |}
    """.trimMargin())
  }

  fun testChangedVariable() {
    doTest("""
      |f <- function(x, y) {
      |  <selection>print(1)
      |  y <- x + y</selection>
      |  print(2 + y)
      |}
    """.trimMargin(), """
      |foo <- function(x, y) {
      |  print(1)
      |  y <- x + y
      |  return(y)
      |}
      |
      |f <- function(x, y) {
      |  y <- foo(x, y)
      |  print(2 + y)
      |}
    """.trimMargin())
  }

  fun testChangedVariable2() {
    doTest("""
      |f <- function(x) {
      |  <selection>x <- x + 1</selection>
      |  x <- x + 2
      |  return(x)
      |}
    """.trimMargin(), """
      |foo <- function(x) {
      |  x <- x + 1
      |  return(x)
      |}
      |
      |f <- function(x) {
      |  x <- foo(x)
      |  x <- x + 2
      |  return(x)
      |}
    """.trimMargin())
  }

  fun testChangedVariableWithParameter() {
    doTest("""
      |f <- function(x) {
      |  z <- 2
      |  <selection>if (x > 0) {
      |    z <- 3
      |  }</selection>
      |  return(x + z)
      |}
    """.trimMargin(), """
      |foo <- function(x, z) {
      |  if (x > 0) {
      |    z <- 3
      |  }
      |  return(z)
      |}
      |
      |f <- function(x) {
      |  z <- 2
      |  z <- foo(x, z)
      |  return(x + z)
      |}
    """.trimMargin())
  }

  fun testBreakInLoop() {
    doTest("""
      |f <- function(x) {
      |  <selection>for (i in 1:x) {
      |    if (i == 5) break
      |  }</selection>
      |}
    """.trimMargin(), """
      |foo <- function(x) {
      |  for (i in 1:x) {
      |    if (i == 5) break
      |  }
      |}
      |
      |f <- function(x) {
      |  foo(x)
      |}
    """.trimMargin())
  }

  fun testGlobal() {
    doTest("""
      |1 + <selection>2 * 3</selection>
    """.trimMargin(), """
      |foo <- function() 2 * 3
      |1 + foo()
    """.trimMargin())
  }

  fun testDefaultParam() {
    doTest("""
      |f <- function(x = <selection>4</selection>) x * 2
    """.trimMargin(), """
      |foo <- function() 4
      |f <- function(x = foo()) x * 2
    """.trimMargin())
  }

  fun testDuplicateExpression() {
    doTest("""
      |f <- function(x, y) {
      |  print(<selection>x + y + 2</selection>)
      |  print(7 * (x * y + x / 2 + 2))
      |}
    """.trimMargin(), """
      |foo <- function(x, y) x + y + 2
      |
      |f <- function(x, y) {
      |  print(foo(x, y))
      |  print(7 * (foo(x * y, x / 2)))
      |}
    """.trimMargin())
  }

  fun testDuplicateBlock() {
    doTest("""
      |f <- function(x, y) {
      |  if (x > 0) {
      |    <selection>a <- x * 3
      |    z <- x + y + a</selection>
      |    return(z)
      |  } else {
      |    print(y)
      |    a <- x * 2 * 3
      |    z <- x * 2 + 5 + a
      |    return(z)
      |  }
      |}
    """.trimMargin(), """
      |foo <- function(x, y) {
      |  a <- x * 3
      |  z <- x + y + a
      |  return(z)
      |}
      |
      |f <- function(x, y) {
      |  if (x > 0) {
      |    z <- foo(x, y)
      |    return(z)
      |  } else {
      |    print(y)
      |    z <- foo(x * 2, 5)
      |    return(z)
      |  }
      |}
    """.trimMargin())
  }

  fun testDuplicateWithReturn() {
    doTest("""
      |f <- function(x, y) {
      |  if (x > 0) {
      |    <selection>z <- x + y
      |    return(z * y)</selection>
      |  } else {
      |    z <- 2 + 3
      |    return(z * 3)
      |  }
      |}
    """.trimMargin(), """
      |foo <- function(x, y) {
      |  z <- x + y
      |  return(z * y)
      |}
      |
      |f <- function(x, y) {
      |  if (x > 0) {
      |    return(foo(x, y))
      |  } else {
      |    return(foo(2, 3))
      |  }
      |}
    """.trimMargin())
  }

  fun testNoopBreak() {
    doTestNoop("""
      |f <- function(x) {
      |  for (i in 1:x) {
      |    <selection>print(1)
      |    break</selection>
      |  }
      |}
    """.trimMargin())
  }

  fun testNoopAssignee() {
    doTestNoop("<selection>x</selection> <- 5")
  }
  fun testNoopMultipleVariables() {
    doTestNoop("""
      |f <- function() {
      |  <selection>x <- 2
      |  y <- 3</selection>
      |  return(x + y)
      |}
    """.trimMargin())
  }

  fun testNoopNamedArgument() {
    doTestNoop("""
      |f <- function(x = 2) x
      |print(f(<selection>x = 2</selection>))
    """.trimMargin())
  }

  fun testNoopNamedParameter() {
    doTestNoop("f <- function(<selection>x = 2</selection>) x")
  }

  fun testNoopMultipleExits() {
    doTestNoop("""
      |f <- function(x) {
      |  <selection>if (x > 0) return(x)</selection>
      |  return(-x)
      |}
    """.trimMargin())
  }

  fun testNoopVariableAndValue() {
    doTestNoop("""
      |f <- function(x) {
      |  y = <selection>{
      |    z <- x
      |    x + 2
      |  }</selection>
      |  return(x + y + z)
      |}
    """.trimMargin())
  }

  private fun doTest(text: String, expected: String) {
    myFixture.configureByText("a.R", text)
    val handler = RExtractMethodHandler()
    handler.invoke(project, myFixture.editor, myFixture.file, null)
    myFixture.checkResult(expected.trim(), true)
  }

  private fun doTestNoop(text: String) {
    myFixture.configureByText("a.R", text)
    val handler = RExtractMethodHandler()
    try {
      handler.invoke(project, myFixture.editor, myFixture.file, null)
      TestCase.fail()
    } catch (ignored: CommonRefactoringUtil.RefactoringErrorHintException) {
    }
  }
}
