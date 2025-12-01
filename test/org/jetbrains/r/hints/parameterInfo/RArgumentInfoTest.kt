/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.hints.parameterInfo.RArgumentInfo
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RArgumentInfoTest : RLightCodeInsightFixtureTestCase() {

  fun testArgumentPermutationInfoExist() {
    val info = argumentPermutationInfoForCall("""
      foo <- function() 42
      fo<caret>o()
    """.trimIndent())
    assertNotNull(info)
  }

  fun testArgumentPermutationInfoNotExist() {
    val info = argumentPermutationInfoForCall("foo()")
    assertNull(info)
  }

  fun testArgumentPermutation() {
    fun doTest(text: String, vararg ind: Int, isValid: Boolean) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(ind.toList(), info!!.argumentPermutationInd)
      assertEquals(ind.toList(), info.argumentPermutationIndWithPipeExpression)
      assertEquals(isValid, info.isValid)
    }

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, 20, 30)
    """.trimIndent(), 0, 1, 2, isValid = true)

    doTest("""
      foo <- function(a, b, c, d) 42
      fo<caret>o(10, a = 20, 30)
    """.trimIndent(), 1, 0, 2, isValid = true)

    doTest("""
      foo <- function(a, b, ..., c) 42
      fo<caret>o(10, 30, 40, r = 50, c = 20, 50, 60, a = 30)
    """.trimIndent(), 1, 2, 2, 2, 3, 2, 2, 0, isValid = true)

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, d = 30, c = 40, 20, 50, 60)
    """.trimIndent(), 0, -1, 2, 1, -1, -1, isValid = false)
  }

  fun testArgumentPermutationWithPipe() {
    fun doTest(text: String, vararg ind: Int, isValid: Boolean) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(ind.toList().drop(1), info!!.argumentPermutationInd)
      assertEquals(ind.toList(), info.argumentPermutationIndWithPipeExpression)
      assertEquals(isValid, info.isValid)
    }

    doTest("""
      foo <- function(a, b, c) 42
      10 %>% fo<caret>o(20, 30)
    """.trimIndent(), 0, 1, 2, isValid = true)

    doTest("""
      foo <- function(a, b, c, d) 42
      10 %>% foo(a = 20, 30)
    """.trimIndent(), 1, 0, 2, isValid = true)

    doTest("""
      foo <- function(a, b, ..., c) 42
      10 %>% fo<caret>o(30, l = 40, c = 20, 50, 60, a = 30)
    """.trimIndent(), 1, 2, 2, 3, 2, 2, 0, isValid = true)

    doTest("""
      foo <- function(a, b, c) 42
      10 %>% fo<caret>o(d = 30, c = 40, 20, 50, 60)
    """.trimIndent(), 0, -1, 2, 1, -1, -1, isValid = false)

    doTest("""
      foo <- function() 42
      10 %>% fo<caret>o(a = 30, 20)
    """.trimIndent(), -1, -1, -1, isValid = false)

    doTest("""
      foo <- function(a) 42
      10 %>% fo<caret>o(a = 30)
    """.trimIndent(), -1, 0, isValid = false)
  }

  fun testArgumentNames() {
    fun doTest(text: String, vararg names: String?) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(names.toList(), info!!.argumentNames)
    }

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, 20, 30)
    """.trimIndent(), "a", "b", "c")

    doTest("""
      foo <- function(a, b, c, d) 42
      fo<caret>o(b = 10, 20, a = 30)
    """.trimIndent(), "b", "c", "a")

    doTest("""
      foo <- function(a, b, ..., d) 42
      fo<caret>o(b = 10, 20, 60, d = 30, 40, m = 50)
    """.trimIndent(), "b", "a", "...", "d", "...", "...")

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, d = 20, 30, 40, 50, c = 40)
    """.trimIndent(), "a", null, "b", null, null, "c")
  }

  fun testArgumentNamesWithPipe() {
    fun doTest(text: String, vararg names: String?) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(names.toList().drop(1), info!!.argumentNames)
      assertEquals(names.toList(), info.argumentNamesWithPipeExpression)
    }

    doTest("""
      foo <- function(a, b, c) 42
      10 %>% fo<caret>o(20, 30)
    """.trimIndent(), "a", "b", "c")

    doTest("""
      foo <- function(a, b, c, d) 42
      20 %>% fo<caret>o(c = 10, a = 30)
    """.trimIndent(), "b", "c", "a")

    doTest("""
      foo <- function(a, b, ..., d) 42
      20 %>% fo<caret>o(b = 10, 60, d = 30, 40, u = 50)
    """.trimIndent(), "a", "b", "...", "d", "...", "...")

    doTest("""
      foo <- function(a) 42
      10 %>% fo<caret>o(10, a = 10, 20)
    """.trimIndent(), null, null, "a", null)
  }

  fun testNotPassedParameters() {
    fun doTest(text: String, ind: List<Int>, names: List<String?>) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(ind.toList(), info!!.notPassedParameterInd)
      assertEquals(names.toList(), info.notPassedParameterNames)
    }

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, 20, 30)
    """.trimIndent(), emptyList(), emptyList())

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(b = 10)
    """.trimIndent(), listOf(0, 2), listOf("a", "c"))

    doTest("""
      foo <- function(a, b, ..., c) 42
      fo<caret>o(10, b = 10)
    """.trimIndent(), listOf(2, 3), listOf("...", "c"))

    doTest("""
      foo <- function(a, b, ..., c) 42
      fo<caret>o(10, b = 10, 20)
    """.trimIndent(), listOf(3), listOf("c"))

    doTest("""
      foo <- function(a, b, c) 42
      100 %>% fo<caret>o(c = 10)
    """.trimIndent(), listOf(1), listOf("b"))

    doTest("""
      foo <- function(a, b, c) 42
      100 |> fo<caret>o(c = 10)
    """.trimIndent(), listOf(1), listOf("b"))

    doTest("""
      foo <- function(a) 42
      42 %>% fo<caret>o(c = 10, a = 10, 10, 20, 30)
    """.trimIndent(), emptyList(), emptyList())

    doTest("""
      foo <- function(a) 42
      42 |> fo<caret>o(c = 10, a = 10, 10, 20, 30)
    """.trimIndent(), emptyList(), emptyList())
  }

  fun testAllDotsArguments() {
    fun doTest(text: String, vararg ind: Int) {
      val call = findCallAtCaret(text)
      val info = argumentPermutationInfoForCall(text, call)
      assertNotNull(info)
      val expressions = info!!.expressionListWithPipeExpression
      val expectedResult = ind.map { expressions[it] }
      assertEquals(expectedResult, info.allDotsArguments)
      assertEquals(expectedResult, RArgumentInfo.getAllDotsArguments(call))
    }

    doTest("""
      foo <- function(a, b, ..., c) 42
      fo<caret>o(10, 20, 30, b = 40, 50, c = 60, 90)
    """.trimIndent(), 1, 2, 4, 6)

    doTest("""
      foo <- function(a, b, ..., c) 42
      50 %>% fo<caret>o(10, l = 20, z = 30, b = 40, a = 50, c = 60, 90)
    """.trimIndent(), 0, 1, 2, 3, 7)

    doTest("""
      foo <- function(a, b, ..., c) 42
      50 |> fo<caret>o(10, l = 20, z = 30, b = 40, a = 50, c = 60, 90)
    """.trimIndent(), 0, 1, 2, 3, 7)

    doTest("""
      foo <- function(a, b) a + b
      fo<caret>o(10, 20)
    """.trimIndent())
  }

  fun testGetArgumentParameterName() {
    fun doTest(text: String, vararg names: String?) {
      val info = argumentPermutationInfoForCall(text)
      assertNotNull(info)
      assertEquals(names.toList(), info!!.expressionListWithPipeExpression.map { info.getParameterNameForArgument(it) })
    }

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, 20, 30)
    """.trimIndent(), "a", "b", "c")

    doTest("""
      foo <- function(a, b, ..., d) 42
      fo<caret>o(b = 10, 20, 30, 60, d = 80, 90)
    """.trimIndent(), "b", "a", "...", "...", "d", "...")

    doTest("""
      foo <- function(a, b, ..., d) 42
      42 %>% fo<caret>o(d = 10, 20, 30, a = 80, 90)
    """.trimIndent(), "b", "d", "...", "...", "a", "...")

    doTest("""
      foo <- function(a, b, ..., d) 42
      42 |> fo<caret>o(d = 10, 20, 30, a = 80, 90)
    """.trimIndent(), "b", "d", "...", "...", "a", "...")

    doTest("""
      foo <- function(a, b, ..., d) 42
      42 %>% fo<caret>o(d = 10, 20, l = 7, a = 80, 90)
    """.trimIndent(), "b", "d", "...", "...", "a", "...")
  }

  fun testGetArgumentPassedToParameter() {
    fun doTest(text: String, vararg names: String?) {
      val call = findCallAtCaret(text)
      val info = argumentPermutationInfoForCall(text, call)
      assertNotNull(info)
      val parameterNames = info!!.parameterNames
      val nameToExpr = names
        .mapIndexedNotNull { ind, name -> (name ?: return@mapIndexedNotNull null) to info.expressionListWithPipeExpression[ind] }
        .toMap()
      val expectedResult = parameterNames.map { nameToExpr[it]?.unfoldNamedArgument() }
      assertEquals(expectedResult, parameterNames.map { info.getArgumentPassedToParameter(it) })
      assertEquals(expectedResult, parameterNames.mapIndexed { ind, _ -> info.getArgumentPassedToParameter(ind) })
      assertEquals(expectedResult, parameterNames.map { RArgumentInfo.getArgumentByName(call, it) })
    }

    doTest("""
      foo <- function(a, b, c) 42
      fo<caret>o(10, 20, 30)
    """.trimIndent(), "a", "b", "c")

    doTest("""
      foo <- function(a, b, c, d) 42
      fo<caret>o(b = 10, 20, a = 30, 60, d = 80, 90)
    """.trimIndent(), "b", "c", "a", null, "d", null)

    doTest("""
      foo <- function(a, b, f) 42
      42 %>% fo<caret>o(10)
    """.trimIndent(), "a", "b")

    doTest("""
      foo <- function(a, b, f) 42
      42 |> fo<caret>o(10)
    """.trimIndent(), "a", "b")
  }

  fun testCheckArgumentParameterNamePassedToParameterInverseOp() {
    fun doTest(text: String) {
      val call = findCallAtCaret(text)
      val info = argumentPermutationInfoForCall(text, call)
      assertNotNull(info)
      val expressions = call.argumentList.expressionList
      assertEquals(expressions.map { it.unfoldNamedArgument() },
                   expressions.map {
                     info!!.getArgumentPassedToParameter(info.getParameterNameForArgument(it) ?: error("Undefined argument ${it.text}"))
                   })

      val filteredNames = info!!.parameterNames.filter { info.getArgumentPassedToParameter(it) != null }
      assertEquals(filteredNames, filteredNames.map { info.getParameterNameForArgument(info.getArgumentPassedToParameter(it)!!) })
    }

    doTest("""
      foo <- function(a, b, c, d, e) 42
      fo<caret>o(10, 20, a = 50, e = 10, 40)
    """.trimIndent())

    doTest("""
      foo <- function(a, b, c, d, e) 42
      42 %>% fo<caret>o(10, a = 50, e = 10, 40)
    """.trimIndent())
  }

  fun testInfoForLeftPipeExpr() {
    val info = argumentPermutationInfoForCall("""
      foo <- function(a) a
      bar <- function(a, b) a + b
      fo<caret>o(-42) %>% bar(42)
    """.trimIndent())
    assertNotNull(info)
    assertEquals(1, info!!.expressionListWithPipeExpression.size)
  }

  private fun argumentPermutationInfoForCall(text: String, call: RCallExpression = findCallAtCaret(text)): RArgumentInfo? {
    return RArgumentInfo.getArgumentInfo(call)
  }

  private fun findCallAtCaret(text: String): RCallExpression {
    val file = myFixture.configureByText("a.R", text)
    val element = file.findElementAt(myFixture.caretOffset)
    return PsiTreeUtil.getParentOfType(element, RCallExpression::class.java) ?: error("No RCallExpression at caret")
  }

  private fun RExpression.unfoldNamedArgument() =  if (this is RNamedArgument) assignedValue else this
}