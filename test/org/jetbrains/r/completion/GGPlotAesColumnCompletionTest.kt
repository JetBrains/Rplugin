/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.interpreter.RInterpreterBaseTestCase

class GGPlotAesColumnCompletionTest : RInterpreterBaseTestCase() {

  fun testCompletionBaseLine() {
    checkCompletionDplyr("""ggplot(table, aes(yyyy_a<caret>))""",
                         initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                         expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }


  fun testCompletionBaseLineNamed() {
    checkCompletionDplyr("""ggplot(table, aes(x = yyyy_a<caret>))""",
                          initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                          expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }

  fun testDataTable() {
    checkCompletionDataTable("""ggplot(table, aes(x = yyyy_a<caret>))""",
                             initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                             expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }

  fun testCompletionNamedArguments() {
    checkCompletionDplyr("""ggplot(mapping = aes(x = yyyy_a<caret>), data = table)""",
                         initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                         expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }

  fun testNoCompletion() {
    checkCompletionDplyr("""ggplot(aes(x = yyyy_a<caret>), table)""",
                         initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                         expected = listOf())
  }

  fun testPlusOperator() {
    checkCompletionDplyr("""ggplot(data = table, aes(yyyy_aa, yyyy_ab)) + geom_points(aes(yyyy_<caret>))""",
                         initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                         expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }

  fun testNonPlusOperator() {
    checkCompletionDplyr("""ggplot(data = table, aes(yyyy_aa, yyyy_ab)) * geom_points(aes(yyyy_<caret>))""",
                         initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                         expected = listOf())
  }

  fun testPlusOperatorVariable() {
    val initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad")
    rInterop.executeCode("library(data.table)", true)
    rInterop.executeCode("table <- data.table(${initial.joinToString(", ") { "$it = NA" }})", false)
    rInterop.executeCode("""
  library(ggplot2)
  foo <- ggplot(data = table, aes(yyyy_aa, yyyy_ab))
  """)
    checkCompletionDataTable("""foo + geom_points(aes(yyyy_<caret>))""",
                         expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"))
  }

  fun testFacetGrid() {
    checkCompletionDplyr("ggplot(table, aes(displ, cty)) + geom_point() + facet_grid(yyy<caret>)",
                         initial = listOf("yyyy_aa", "yyyy_ab"),
                         expected = listOf("yyyy_aa", "yyyy_ab"))
  }


  fun testFacetWrap() {
    checkCompletionDplyr("ggplot(table, aes(displ, cty)) + geom_point() + facet_wrap(yyy<caret>)",
                         initial = listOf("yyyy_aa", "yyyy_ab"),
                         expected = listOf("yyyy_aa", "yyyy_ab"))
  }

  fun testInQplot() {
    checkCompletionDplyr(
      """
        my_table <- tibble(custom_column_1 = norm(10), custom_column_2 = norm(10))
        qplot(x = custom_colum<caret>, y = custom_column_2, data = my_table, geom = "point")
      """.trimIndent(),
      listOf("custom_column_1", "custom_column_2"),
      listOf()
    )
  }

  private fun checkCompletionDataTable(text: String,
                                        expected: List<String> = emptyList(),
                                        initial: List<String>? = null,
                                        notExpected: List<String>? = null) {
    rInterop.executeCode("library(data.table)", true)
    if (initial != null) {
      rInterop.executeCode("table <- data.table(${initial.joinToString(", ") { "$it = NA" }})", false)
    }
    myFixture.configureByText("a.R", text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic().toList()
    assertNotNull(result)

    val lookupStrings = result.map { it.lookupString }
    lookupStrings.filter { expected.contains(it) }.apply {
      assertEquals(distinct(), this)
    }

    assertContainsElements(lookupStrings, expected)
    if (notExpected != null) {
      assertDoesntContain(lookupStrings, notExpected)
    }
  }

  private fun checkCompletionDplyr(text: String, expected: List<String>, initial: List<String>? = null, initialGroups: List<String>? = null) {
    rInterop.executeCode("library(dplyr)", true)
    if (initial != null) {
      rInterop.executeCode("table <- dplyr::tibble(${initial.joinToString(", ") { "$it = NA" }})", false)
      if (initialGroups != null) {
        rInterop.executeCode("table <- dplyr::group_by(table, ${initialGroups.joinToString(", ")})", false)
      }
    } else {
      rInterop.executeCode("table <- NULL", false)
    }
    myFixture.configureByText("a.R", text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }.filter { it != "table" }
    TestCase.assertEquals(expected, lookupStrings)
  }
}