/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class DplyrCompletionTest : RProcessHandlerBaseTestCase() {
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

  private fun checkCompletion(text: String, expected: List<String>, initial: List<String>? = null, initialGroups: List<String>? = null) {
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