/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.psi.api.RAssignmentStatement

class RInlineLocalTest : RUsefulTestCase() {

  fun testInlineNumber() {
    doTest("inline <- 4\ninline + inline", "4 + 4")
    doTest("inline <- 8\ninline + inline", "8 + 8")
  }

  fun testInlineNaNanNullInf() {
    doTest("a <- NA\n5 + a + 3", "5 + NA + 3")
    doTest("a <- NA_complex_\n5 + a + 3", "5 + NA_complex_ + 3")
    doTest("a <- NaN\n5 + a + 3", "5 + NaN + 3")
    doTest("a <- NULL\n5 + a + 3", "5 + NULL + 3")
    doTest("a <- Inf\n5 + a + 3", "5 + Inf + 3")
  }

  fun testInlineString() {
    doTest("a <- 'testString'\na", "'testString'")
  }

  fun testInlineArithmeticOperators() {
    doTest("sum <- i + 1\nsum * 10", "(i + 1) * 10")
    doTest("sum <- i * 1\nsum * 10", "i * 1 * 10")
    doTest("sum <- 5 + 3\n10 - sum", "10 - (5 + 3)")
    doTest("sum <- 5 + 3\nsum - 10", "5 + 3 - 10")
  }

  fun testInlineLogicalOperators() {
    doTest("qe <- i > 1\nqe == (m < t)", "i > 1 == (m < t)")
    doTest("qe <- i > 1\n(m < t) == qe", "(m < t) == (i > 1)")
    doTest("eq <- a == b\n!eq", "!a == b")
    doTest("eq <- F || T\nF && eq", "F && (F || T)")
  }

  fun testManyAssignments() {
    doTest("""
      a <- 3
      a + 15
      a <- 6
      <caret>a + 3
      100 - a
      a <- 7
      4 + a
    """.trimIndent(), """
      a <- 3
      a + 15
      6 + 3
      100 - 6
      a <- 7
      4 + a
    """.trimIndent())

    doTest("""
      a <- 3
      a <- 6
      a + 3
      <caret>a <- 7
      4 + a
    """.trimIndent(), """
      a <- 3
      a <- 6
      a + 3
      4 + 7
    """.trimIndent())
  }

  fun testNamedArgument() {
    val tests = genTests("""
      <0>a <- 3
      myFoo(<1>a = <2>a + <3>a)
      4 + <4>a
    """, 5)

    for (i in 2 until 5) {
      if (i == 1) {
        doTest(tests[i].trimIndent(), """
            a <- 3
            myFoo(a = a + a)
            4 + a
          """.trimIndent())
      } else {
        doTest(tests[i].trimIndent(), """
        myFoo(a = 3 + 3)
        4 + 3
      """.trimIndent())
      }
    }
  }

  fun testForVariable() {
    doTest("""
      a <- 3
      for (b in 1:5) { a + 3 }
      4 + a
    """.trimIndent(), """
      for (b in 1:5) { 3 + 3 }
      4 + 3
    """.trimIndent())

    doTest("""
      a <- 3
      for (a in 1:5) { a + 3 }
      4 + a
    """.trimIndent(), """
      for (a in 1:5) { a + 3 }
      4 + a
    """.trimIndent())
  }

  fun testIfStatement() {
    doTest("""
      if (T) {
        a <- 5
      }
      <caret>a + 3
    """.trimIndent(), """
      if (T) {
      }
      5 + 3
    """.trimIndent())

    doTest("""
      if (T) {
        a <- 5
      }
      else {
        b <- 4
      }
      <caret>a + 3
    """.trimIndent(), """
      if (T) {
      }
      else {
        b <- 4
      }
      5 + 3
    """.trimIndent())

    doTest("""
      if (T) {
        b <- 4
      }
      else {
        a <- 5
      }
      <caret>a + 3
    """.trimIndent(), """
      if (T) {
        b <- 4
      }
      else {
      }
      5 + 3
    """.trimIndent())
  }

  fun testManyDefs() {
    fun helper(text: String, subTestCount: Int = 3) {
      val tests = genTests(text, subTestCount)
      for (i in 0 until subTestCount) {
        doAssertionTest {
          doTest(tests[i].trimIndent(), "")
        }
      }
    }

    helper("""
      <0>a <- 3
      if (T) {
        <1>a <- 4
      }
      <2>a + 3
    """)

    helper("""
      if (T) {
        <0>a <- 4
      }
      else {
        <1>a <- 5
      }
      <2>a + 3
    """)

    helper("""
      <0>a <- 3
      for (i in 1:5) {
        <1>a <- 4
      }
      <2>a + 3
    """)
  }

  fun testInlineThisOnly() {
    doTest("""
      a <- 3
      a <- 6
      a + <caret>a
      a + 7
    """.trimIndent(), """
      a <- 3
      a <- 6
      a + 6
      a + 7
    """.trimIndent(), true)

    doTest("""
      a <- 3
      <caret>a + a
      a + 7
    """.trimIndent(), """
      a <- 3
      3 + a
      a + 7
    """.trimIndent(), true)
  }

  private fun doTest(text: String, expected: String, inlineThisOnly: Boolean = false) {
    val file = myFixture.configureByText("a.r", text)

    val rule = PsiTreeUtil.getChildOfType(file, RAssignmentStatement::class.java)
               ?: TargetElementUtil.findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted())
    assertNotNull(rule)
    RInlineAssignmentHandler().withInlineThisOnly(inlineThisOnly).inlineElement(project, myFixture.editor, rule!!)
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
