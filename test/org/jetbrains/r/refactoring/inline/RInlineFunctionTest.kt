/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.psi.api.RAssignmentStatement

class RInlineFunctionTest : RUsefulTestCase() {

  fun testInlineLambda() {
    doTest("""
      f <- function() {}
      f<caret>
    """.trimIndent(), """
      function() {}
    """.trimIndent())

    doTest("""
      f <- function(x) { x * x }
      lapply(list(1, 2, 3), f<caret>)
    """.trimIndent(), """
      lapply(list(1, 2, 3), function(x) { x * x })
    """.trimIndent())
  }

  fun testSingleReturn() {
    doTest("""
      f <- function() {
        42
      }
      f()
    """.trimIndent(), """
      42
    """.trimIndent())

    doTest("""
      f <- function() {
        return(20 + 22)
      }
      f() * 10
    """.trimIndent(), """
      (20 + 22) * 10
    """.trimIndent())

    doTest("""
      f <- function() {
        base::return((20 + 22))
      }
      f() * 10
    """.trimIndent(), """
      (20 + 22) * 10
    """.trimIndent())
  }

  fun testManyReturns() {
    doTest("""
      f <- function(a, b) {
        if (a) {
          if (b) {
            45
          }
          else 10 + 12
        }
        else NaN
      }
      f(T, F)
    """.trimIndent(), """
      if (T) {
        if (F) {
          result <- 45
        }
        else result <- 10 + 12
      }
      else result <- NaN
      result
    """.trimIndent())
  }

  fun testImplicitNullReturn() {
    doTest("""
      f <- function() {}
      f()
    """.trimIndent(), """
    """.trimIndent())

    doTest("""
      f <- function() {}
      f() + 15
    """.trimIndent(), """
      NULL + 15
    """.trimIndent())
  }

  fun testImplicitNullReturnInFor() {
    doTest("""
      f <- function() {
        b <- 10
        for (a in 1:10) {
          b <- b + b
        }
      }
      f()
    """.trimIndent(), """
      b <- 10
      for (a in 1:10) {
        b <- b + b
      }
    """.trimIndent())

    doTest("""
      f <- function() {
        b <- 10
        for (a in 1:10) {
          b <- b + b
        }
      }
      f<caret>() + 10
    """.trimIndent(), """
      b <- 10
      for (a in 1:10) {
        b <- b + b
      }
      NULL + 10
    """.trimIndent())
  }

  fun testComplicatedArgument() {
    doTest("""
      f <- function(a, b) {
        a + b(a)
      }
      
      f(42 + 42, function(x) { x * x })
    """.trimIndent(), """
      a <- 42 + 42
      b <- function(x) { x * x }
      a + b(a)
    """.trimIndent())
  }

  fun testInlineFunctionWithSingleReturn() {
    doAssertionTest { doTest("""
      f <- function(a, b) {
        if (a > 0) 
          return()
        print(a + b) 
      }
      
      f()
    """.trimIndent(), "") }
  }

  fun testRename() {
    doTest("""
      f <- function(a) {
        b <- 0
        if (T) a else b
      }
      a <- 0
      f(10)
      b <- 15
      f(6 + 3)
    """.trimIndent(), """
      a <- 0
      b2 <- 0
      if (T) result1 <- 10 else result1 <- b2
      result1
      b <- 15
      a1 <- 6 + 3
      b1 <- 0
      if (T) result <- a1 else result <- b1
      result
    """.trimIndent())
  }

  fun testDefaultValues() {
    doTest("""
      f <- function(a = 10, b = 42) {
        a + b
      }
      f(5)
    """.trimIndent(), """
      5 + 42
    """.trimIndent())
  }

  fun testDotsArguments() {
    doTest("""
      f <- function(a, b, ...) {
        f1(...)
        f2(b, a, ...)
      }
      a <- 3
      b <- 4
      f(a, b, 10, 42, 43)
    """.trimIndent(), """
      a <- 3
      b <- 4
      f1(10, 42, 43)
      f2(b, a, 10, 42, 43)
    """.trimIndent())

    doTest("""
      f <- function(a, ...) {
        f1(...)
      }
      dotArg1 <- 0
      f(a, 10 + 15, 60 * 5)
    """.trimIndent(), """
      dotArg1 <- 0
      dotArg <- 10 + 15
      dotArg2 <- 60 * 5
      f1(dotArg, dotArg2)
    """.trimIndent())
  }

  fun testNamedArguments() {
    doTest("""
      f <- function(a, ..., b = 10) {
        f1(...)
        b - a
      }
      f(10, 5, 6, b = 100, 8)
    """.trimIndent(), """
      f1(5, 6, 8)
      100 - 10
    """.trimIndent())
  }

  fun testShuffledArguments() {
    doTest("""
      f <- function(a, b, c, d) {
        a + b + c + d
      }
      f(d = 1, 2, c = 3, 4)
    """.trimIndent(), """
      2 + 4 + 3 + 1
    """.trimIndent())
  }

  fun testInsideCall() {
    doTest("""
      f <- function(a) {
        b <- 4
        ac <- 5
        a + ac - b
      }
      a(6, 15 + bb(a = f(19)))
    """.trimIndent(), """
      b <- 4
      ac <- 5
      a(6, 15 + bb(a = 19 + ac - b))
    """.trimIndent())
  }

  fun testFunctionWithRecursiveCall() {
    doAssertionTest {
      doTest("""
        f <- function(a) {
          if (a < 5) a else f(a - 5)
        }
        
        f(100)
      """.trimIndent(), "")
    }

    doAssertionTest {
      doTest("""
        f <- function() {
          a + b + f() + c + d
        }
        
        f()
      """.trimIndent(), "")
    }
  }

  fun testFunctionWithInteropControlFlow() {
    doAssertionTest {
      doTest("""
        f <- function() {
          if (T) return 15
          10 + 10
        }
        
        f()
      """.trimIndent(), "")
    }

    doAssertionTest {
      doTest("""
        f <- function() {
          if (T) {
            if (F) 15
          }
          else {
            if (F) 15 else 10
          }
        }
        
        f()
      """.trimIndent(), "")
    }
  }

  fun testTooManyDeclarations() {
    doAssertionTest {
      doTest("""
        f <- function() {)
        if (T) f <- 5
        
        <caret>f()
      """.trimIndent(), "")
    }
  }

  fun testFunWithFunInside() {
    doAssertionTest {
      doTest("""
        f <- function() {
          a <- 15
          f1 <- function() {
            10
          }
          c + 10
        }
      """.trimIndent(), "")
    }
  }

  fun testInlineThisOnly() {
    doTest("""
      f <- function() {
        15
      }
      
      f() + 10
      <caret>f() + 15
      f() + 16
    """.trimIndent(), """
      f <- function() {
        15
      }
      
      f() + 10
      15 + 15
      f() + 16
    """.trimIndent(), inlineThisOnly = true)
  }

  fun testKeepDeclaration() {
    doTest("""
      f <- function() {
        15
      }
      
      f() + 10
      f() + 15
      f() + 16
    """.trimIndent(), """
      f <- function() {
        15
      }
      
      15 + 10
      15 + 15
      15 + 16
    """.trimIndent(), removeDeclaration = false)
  }

  fun testNotAllArgumentsPassed() {
    doTest("""
      f <- function(a, b, c) {
        a + b + c
      }
      
      f(10, c = 15)
    """.trimIndent(), """
      10 + b + 15
    """.trimIndent())
  }

  fun testUseMethod() {
    addLibraries()
    doAssertionTest {
      doTest("""
        f <- function(x, ...) {
          UseMethod("f")
        }
        
        <caret>f()
      """.trimIndent(), "")
    }
  }

  private fun doTest(text: String, expected: String, inlineThisOnly: Boolean = false, removeDeclaration: Boolean = true) {
    val file = myFixture.configureByText("a.r", text)

    val rule = PsiTreeUtil.getChildOfType(file, RAssignmentStatement::class.java)
               ?: TargetElementUtil.findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted())
    assertNotNull(rule)
    RInlineAssignmentHandler().withInlineThisOnly(inlineThisOnly).withRemoveDefinition(removeDeclaration)
      .inlineElement(project, myFixture.editor, rule!!)
    assertSameLines(expected, file.text)
  }

  private fun genTests(text: String, cnt: Int): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until cnt) {
      result.add(text.replace("<$i>", "<caret>").replace(Regex("<\\d+>"), ""))
    }
    return result.toList()
  }

  private fun doAssertionTest(runTest: () -> Unit) {
    assertThrows(CommonRefactoringUtil.RefactoringErrorHintException::class.java) {
        runTest()
      }
  }
}
