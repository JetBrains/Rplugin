/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

class DplyrCompletionTest : RColumnCompletionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
    rInterop.executeCode("library(dplyr)", true)
  }

  fun testLoader() {
    checkCompletion("table %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aba", "yyyy_aca", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aba", "yyyy_aca", "yyyy_add"))
  }

  fun testTibble() {
    checkCompletion("tibble(yyyy_aa = 1, yyyy_ab = 1, yyyy_ac = 1) %>% filter(yyyy_a<caret>)",
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac"))
  }

  fun testTibbleDoesNotHaveTableArgument() {
    checkCompletion("tibble(table, yyyy_ax = 0, yyyy_ay = 1, yyyy_ac = yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab"),
                    expected = listOf("yyyy_ax", "yyyy_ay"))
  }

  fun testNoSideEffects() {
    rInterop.executeCode("xyz <- 15", true)
    checkCompletion("filter((xyz <- tibble(yyyy_ab = 1)), yyyy_a<caret>)", expected = listOf())
    TestCase.assertEquals("15", rInterop.executeCode("cat(xyz)", true).stdout)

    checkCompletion("tibble(xyz = c(\"yyyy<caret>\"))", expected = listOf())
    TestCase.assertEquals("15", rInterop.executeCode("cat(xyz)", true).stdout)

    rInterop.executeCode("foo <- function() { xyz <<- 25; tibble(yyyy_ac = 2) }", true)
    checkCompletion("filter(foo(), yyyy_a<caret>)", expected = listOf())
    TestCase.assertEquals("15", rInterop.executeCode("cat(xyz)", true).stdout)

    rInterop.executeCode("`%foo%` <- function(x, y) { xyz <<- x + y; tibble(yyyy_ad = 2) }", true)
    checkCompletion("filter(20 %foo% 30, yyyy_a<caret>)", expected = listOf())
    TestCase.assertEquals("15", rInterop.executeCode("cat(xyz)", true).stdout)
  }

  fun testTibbleInside() {
    checkCompletion("tibble(yyyy_aa = 1, yyyy_ab = 1, yyyy_ac = yyyy_a<caret>, yyyy_ad = 1)",
                    expected = listOf("yyyy_aa", "yyyy_ab"))
  }

  fun testSelect() {
    checkCompletion("table %>% select(yyyy_aa, yyyy_ac, yyyy_az = yyyy_ad) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ac", "yyyy_az"))
  }

  fun testRename() {
    checkCompletion("table %>% rename(yyyy_az = yyyy_ab) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ac", "yyyy_ad", "yyyy_az"))
  }

  fun testMutate() {
    checkCompletion("table %>% mutate(yyyy_ac = 2, yyyy_az = 3 + 4, yyyy_ay = yyyy_az) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad", "yyyy_ay", "yyyy_az"))
  }

  fun testTransmute() {
    checkCompletion("table %>% transmute(yyyy_ac = 2, yyyy_az = 3 + 4, yyyy_ay = yyyy_az) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_ac", "yyyy_ay", "yyyy_az"))
  }

  fun testGroupBySummarise() {
    checkCompletion("table %>% group_by(yyyy_aa, yyyy_ab) %>% summarise(yyyy_az = mean(yyyy_ac)) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_az"))
  }

  fun testGroupBySummariseSummarise() {
    checkCompletion(
      """table %>%
        |  group_by(yyyy_aa, yyyy_ab, yyyy_ac) %>%
        |  summarise(yyyy_az = mean(yyyy_ad)) %>%
        |  summarise(yyyy_az = sum(yyyy_az)) %>%
        |  filter(yyyy_a<caret>)""".trimMargin(),
      initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
      expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_az")
    )
  }

  fun testCurrentColumns() {
    checkCompletion("table %>% mutate(yyyy_ay = yyyy_aa, yyyy_az = yyyy_a<caret>, yyyy_ax = yyyy_ab)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad", "yyyy_ay"))
  }

  fun testNestedStyle() {
    checkCompletion("transmute(mutate(table, yyyy_ac = 1 + 2), yyyy_ax = 1, yyyy_ay = yyyy_a<caret>, yyyy_az = 2)",
                    initial = listOf("yyyy_aa", "yyyy_ab"),
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ax"))
  }

  fun testLoadGrouped() {
    checkCompletion("table %>% summarise(yyyy_az = sum(yyyy_ab + yyyy_ac)) %>% filter(yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    initialGroups = listOf("yyyy_aa", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ad", "yyyy_az"))
  }

  fun testSubscription() {
    checkCompletion("""table[c("yyyy_aa", "yyyy_ac")] %>% filter(yyyy_a<caret>)""",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf("yyyy_aa", "yyyy_ac"))
    checkCompletion("""table["yyyy_ac"] %>% filter("yyyy_a<caret>")""",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac", "yyyy_ad"),
                    expected = listOf())
  }

  fun testSubscriptionInside() {
    checkCompletion("""table["yyyy_a<caret>"]""",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac"),
                    expected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac"))
    checkCompletion("""table[yyyy_a<caret>]""",
                    initial = listOf("yyyy_aa", "yyyy_ab", "yyyy_ac"),
                    expected = listOf("\"yyyy_aa\"", "\"yyyy_ab\"", "\"yyyy_ac\""))
  }

  fun testDataFrame() {
    rInterop.executeCode("t <- data.frame(yyyy_aaa = NA, yyyy_aab = NA)", true)
    checkCompletion("t %>% filter(yyyy_<caret>)",
                    expected = listOf("yyyy_aaa", "yyyy_aab"))
    checkCompletion("t[yyyy_<caret>]", expected = listOf("\"yyyy_aaa\"", "\"yyyy_aab\""))
  }

  fun testNamedTableArgument() {
    checkCompletion("filter(yyyy_a<caret>, .data = table)",
      initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
      expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add"))
  }

  fun testTwoTableArguments() {
    rInterop.executeCode("x <- tibble(yyyy_aba = 1:5, yyyy_ada = 6:10)", true)
    rInterop.executeCode("y <- tibble(yyyy_aca = -1:-5, yyyy_ara = -6:-10)", true)

    checkCompletion("full_join(x, y, by=c(yyyy_a<caret>))",
                    expected = listOf("\"yyyy_aba\"", "\"yyyy_aca\"", "\"yyyy_ada\"", "\"yyyy_ara\""))
  }

  fun testTwoTableArgumentsWithSameColumns() {
    rInterop.executeCode("x <- tibble(yyyy_aba = 1:5, yyyy_ada = 6:10)", true)
    rInterop.executeCode("y <- tibble(yyyy_aba = -1:-5, yyyy_ara = -6:-10)", true)

    checkCompletion("full_join(x, y, by=c(yyyy_a<caret>))",
                    expected = listOf("\"yyyy_aba\"", "\"yyyy_ada\"", "\"yyyy_ara\""))
  }

  fun testStarwarsColumnsFirst() {
    myFixture.configureByText("a.R", "starwars %>% filter(<caret>)")
    addRuntimeInfo()
    val columns = arrayOf("birth_year", "eye_color", "films", "gender", "hair_color",
                          "height", "homeworld", "mass", "name", "n", "skin_color",
                          "species", "starships", "stars", "vehicles")
    val completeBasic = myFixture.completeBasic()
    UsefulTestCase.assertContainsElements(completeBasic.take(columns.size + 1).map { it.toString() }, *columns)
  }

  fun testNamedArgumentIsNotColumn() {
    rInterop.executeCode("table <- dplyr::tibble()")
    myFixture.configureByText("a.R", "table %>% count(name = 'someName', nam<caret>)")
    addRuntimeInfo()
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }.filter { it != "table" }
    assertEquals(1, lookupStrings.count { it == "name" })
  }

  fun testStaticLoadedColumnsFromTibble() {
    checkStaticCompletion(
      "table <- dplyr::tibble(my_column = letters) %>% count(my<caret>)",

      listOf("my_column"),
      listOf()
    )
  }

  fun testStaticLoadingColumnsInChainedCall() {
    checkStaticCompletion(
      "dplyr::tibble(my_column = letters, another_column = letters) %>% dplyr::filter(my_column == 'a') %>% count(my_column) %>% dplyr::filter(m<caret>)",

      listOf("my_column"),
      listOf("another_column")
    )
  }

  fun testStaticLoadingColumnsInThroughVariables() {
    checkStaticCompletion(
      "tbl.1 <- dplyr::tibble(my_column = letters, another_column = letters)\n" +
      "tbl.2 <- tbl.1 %>% dplyr::filter(my_column == 'a')\n" +
      "tbl.2 %>% count(my_column) %>% dplyr::filter(<caret>)",

      listOf("my_column"),
      listOf("another_column")
    )
  }

  fun testStaticCompletionCount() {
    checkStaticCompletion(
      "tbl.1 <- dplyr::tibble(my_column = letters, another_column = letters)\n" +
      "tbl.1 %>% dplyr::count(my_column) %>% dplyr::filter(<caret>)",

      listOf("my_column", "n"),
      listOf("another_column")
    )
  }

  fun testStaticCompletionCountWithColumnName() {
    checkStaticCompletion(
      "tbl.1 <- dplyr::tibble(my_column = letters, another_column = letters)\n" +
      "tbl.1 %>% dplyr::count(my_column, name = \"count_column\") %>% dplyr::filter(<caret>)",

      listOf("my_column", "count_column"),
      listOf("n", "another_column")
    )
  }

  fun testStaticCompletionMutate() {
    checkStaticCompletion(
      "mtcars %>% dplyr::mutate(cyl2 = cyl * 2) %>% dplyr::filter(<caret>)",

      listOf("cyl2"),
      listOf()
    )
  }

  fun testStaticCompletionColumnN() {
    checkStaticCompletion(
      "tbl <- dplyr::tibble(n = letters, nn = letters, custom = letters)\n" +
      "tbl %>% dplyr::count(n, nn) %>% dplyr::mutate(<caret>)",

      listOf("nnn"),
      listOf()
    )
  }

  fun testStaticCompletionWithSeveralAssignments() {
    checkStaticCompletion(
      "tbl <- dplyr::tibble(my_column = letters, another_column = letters)\n" +
      "tbl <- tbl %>% dplyr::mutate(third_column = letters)\n" +
      "tbl %>% dplyr::filter(<caret>)",

      listOf("third_column"),
      listOf()
    )
  }

  fun testStaticCompletionInFunction() {
    checkStaticCompletion(
      "f <- function() {\n" +
      " tbl <- dplyr::tibble(my_column = letters)\n" +
      " tbl %>% dplyr::filter(<caret>)\n" +
      "}",

      listOf("my_column"),
      listOf()
    )
  }

  fun testStaticCompletionInFunctionWithReferenceToGlobalVariable() {
    checkStaticCompletion(
      "tbl <- dplyr::tibble(my_column = letters)\n" +
      "f <- function() {\n" +
      " tbl %>% dplyr::filter(<caret>)\n" +
      "}",

      listOf("my_column"),
      listOf()
    )
  }

  fun testStaticCompletionInNestedFunctions() {
    checkStaticCompletion(
      """
        transmute(mutate(table, yyyy_ac = 1 + 2), yyyy_ax = 1, yyyy_ay = yyyy_<caret>, yyyy_az = 2)        
      """.trimIndent(),

      listOf("yyyy_ac"),
      listOf()
    )
  }

  fun testStaticCompletionForSelect() {
    checkStaticCompletion(
      """
        a1 <- select(starwars, name, height, mass)
        a2 <- mutate(a1, abc = 2 * heig<caret>)
      """.trimIndent(),

      listOf("height"),
      listOf("hair_color")
    )
  }

  fun testStaticCompletionForJoin() {
    checkStaticCompletion(
      """
tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
tbl2 <- tibble(letter = letters, column_two = letters)

tbl1 %>% inner_join(tbl2) %>% filter(colu<caret>)
      """.trimIndent(),

      listOf("column_one", "column_two"),
      listOf()
    )
  }

  fun testStaticCompletionForRelocate() {
    checkStaticCompletion(
      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% relocate(column_one) %>% filter(colu<caret>)
      """.trimIndent(),

      listOf("column_one"),
      listOf()
    )
  }

  fun testStaticCompletionForRowwise() {
    checkStaticCompletion(
      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% dplyr::rowwise() %>% filter(colu<caret>)
      """.trimIndent(),

      listOf("column_one"),
      listOf()
    )
  }

  fun testStaticCompletionForSelectEverything() {
    checkStaticCompletion(
      """
      tbl1 <- dplyr::tibble(letter = letters, column_one = letters)
      tbl1 %>% dplyr::select(everything()) %>% dplyr::filter(column<caret>)
      """.trimIndent(),

      listOf("column_one"),
      listOf()
    )
  }

  fun testStaticCompletionInGgplot() {
    checkStaticCompletion(
      """
        library(ggplot2)
        library(magrittr)
        library(dplyr)

        my_table <- mpg
        my_table <- my_table %>% dplyr::mutate(sprarg = cyl)

        ggplot(my_table, aes(spr<caret>, hwy, colour = class)) + geom_point()
      """.trimIndent(),

      listOf("sprarg"),
      listOf()
    )
  }

  private fun checkCompletion(text: String, expected: List<String>, initial: List<String>? = null, initialGroups: List<String>? = null) {
    if (initial != null) {
      rInterop.executeCode("table <- dplyr::tibble(${initial.joinToString(", ") { "$it = NA" }})", false)
      if (initialGroups != null) {
        rInterop.executeCode("table <- dplyr::group_by(table, ${initialGroups.joinToString(", ")})", false)
      }
    } else {
      rInterop.executeCode("table <- NULL", false)
    }
    myFixture.configureByText("a.R", text)
    addRuntimeInfo()
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map {
      val elementPresentation = LookupElementPresentation()
      it.renderElement(elementPresentation)
      elementPresentation.itemText!!
    }.filter { it != "table" }
    TestCase.assertEquals(expected, lookupStrings)
  }
}