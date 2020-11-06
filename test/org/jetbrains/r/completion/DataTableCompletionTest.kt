/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo

class DataTableCompletionTest : RColumnCompletionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
    rInterop.executeCode("library(data.table)", true)
  }

  fun testSubscription() {
    checkCompletion("table[yyyy_a<caret>])",
                    initial = listOf("yyyy_aba", "yyyy_aca", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aba", "yyyy_aca", "yyyy_add"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testEmptySubscription() {
    checkCompletion("table[][yyyy_a<caret>])",
                    initial = listOf("yyyy_aba", "yyyy_aca", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aba", "yyyy_aca", "yyyy_add"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testInitialize() {
    checkCompletion("data.table(yyyy_aaa = 1, yyyy_abb = 1, yyyy_acc = 1)[yyyy_a<caret>]",
                    expected = listOf("yyyy_aaa", "yyyy_abb", "yyyy_acc"))
  }

  fun testSetKey() {
    checkCompletion("setkey(table, yyyy_a<caret>)",
                    initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testTranspose() {
    rInterop.executeCode("table <- data.table(VALUE = 1:3)", true)
    checkCompletion("transpose(table)[V<caret>])",
                    expected = listOf("V1", "V2", "V3"),
                    notExpected = listOf("VALUE"))
  }

  fun testDuplicates() {
    rInterop.executeCode("table <- data.table(yyyy_aaa = 1:5, yyyy_aaa = -1:-5, yyyy_aaa = LETTERS[1:5], yyyy_abb = LETTERS[1:5])", true)
    checkCompletion("table[yyyy_a<caret>])",
                    expected = listOf("yyyy_aaa", "yyyy_abb"))

  }

  fun testQuoteNeeded() {
    checkCompletion("setkeyv(table, cols = c(a<caret>))",
                    initial = listOf("yyyy_aaa", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("\"yyyy_aaa\"", "\"yyyy_add\""),
                    notExpected = listOf("yyyy_aaa", "yyyy_add", "yyyy_bbc", "\"yyyy_bbc\""))
  }

  fun testRename() {
    checkCompletion("table[, .(yyyy_aba = yyyy_aaa, yyyy_ada = yyyy_add)][yyyy_a<caret>]",
                    initial = listOf("yyyy_aaa", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aba", "yyyy_ada"),
                    notExpected = listOf("yyyy_aaa", "yyyy_add", "yyyy_bbc"))
  }

  fun testTwoTableArguments() {
    rInterop.executeCode("x <- data.table(yyyy_aba = 1:5, yyyy_st = 6:10, yyyy_end = 11:15)", true)
    rInterop.executeCode("y <- data.table(yyyy_aca = -1:-5, yyyy_aba = LETTERS[1:5], yyyy_st = -11:-15, yyyy_end = -6:-10)", true)
    rInterop.executeCode("setkey(y, yyyy_ca, yyyy_st, yyyy_end)", true)

    checkCompletion("foverlaps(x, y, by.x=c(yyyy_a<caret>))",
                    expected = listOf("\"yyyy_aba\"", "\"yyyy_aca\""),
                    notExpected = listOf("\"yyyy_st\"", "\"yyyy_end\""))
  }

  fun testTwoTableArgumentsWithSameColumns() {
    rInterop.executeCode("x <- data.table(yyyy_aba = 1:5, yyyy_aca = 6:10)", true)
    rInterop.executeCode("y <- data.table(yyyy_aba = -1:-5, yyyy_aca = LETTERS[1:5])", true)

    // For execution "aca" should have the same type, but for completion it's not important. This test also checks duplicates
    checkCompletion("funion(x, y, all = is.na(yyyy_a<caret>))",
                    expected = listOf("\"yyyy_aba\"", "\"yyyy_aca\""))
  }

  fun testNamedTableArgument() {
    checkCompletion("setkey(yyyy_a<caret>, x = table)",
                    initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testSubscriptionMixedArgumentsOrder() {
    checkCompletion("table[j = yyyy_aaa, by = .(yyyy_aac), mult = \"all\", yyyy_a<caret>]",
                    initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add"),
                    notExpected = listOf("yyyy_bbc"))

    checkCompletion("table[j = yyyy_aaa, by = .(yyyy_aac), i = yyyy_aac == 5, yyyy_a<caret>]",
                    initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
                    expected = listOf("\"yyyy_aaa\"", "\"yyyy_aac\"", "\"yyyy_add\""),
                    notExpected = listOf("\"yyyy_bbc\""))
  }

  fun testMixedArgumentsOrder() {
    rInterop.executeCode("x <- data.table(yyyy_aba = 1:5, yyyy_st = 6:10, yyyy_end = 11:15)", true)
    rInterop.executeCode("y <- data.table(yyyy_aca = -1:-5, yyyy_ada = LETTERS[1:5], yyyy_st = -11:-15, yyyy_end = -6:-10)", true)
    rInterop.executeCode("setkey(y, yyyy_aca, yyyy_st, yyyy_end)", true)

    checkCompletion("foverlaps(y = y, c(yyyy_a<caret>), x = x)",
                    expected = listOf("\"yyyy_aba\"", "\"yyyy_aca\"", "\"yyyy_ada\""),
                    notExpected = listOf("\"yyyy_st\"", "\"yyyy_end\""))
  }

  fun testMixedArgumentsOrderWithDataTable() {
    checkCompletion("setkey(yyyy_a<caret>, x = data.table(yyyy_aaa = 1:3, yyyy_aba = 4:6, yyyy_bbc = 7:9))",
                    expected = listOf("yyyy_aaa", "yyyy_aba"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testGrouping() {
    rInterop.executeCode("table <- data.table(yyyy_aaa = c(1, 1, 1, 2, 2, 2), yyyy_aab = 1:6, yyyy_aac = c(1, 2, 1, 2, 1 ,2))", true)
    checkCompletion("table[, .(yyyy_aad = .N), by = .(yyyy_aaa, yyyy_aac)][yyyy_aa<caret>]",
                    expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_aad"),
                    notExpected = listOf("yyyy_aab"))
  }

  fun testSubscriptionNested() {
    checkCompletion("`[.data.table`(table, yyyy_aa<caret>)",
                    initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_bbc"),
                    expected = listOf("yyyy_aaa", "yyyy_aac"),
                    notExpected = listOf("yyyy_bbc"))
  }

  fun testNoSideEffects() {

    val checkXyz = { assertEquals("42", rInterop.executeCode("cat(xyz)", true).stdout) }

    rInterop.executeCode("xyz <- 42", true)
    checkCompletion("(xyz <- data.table(yyyy_ab = 1))[yyyy_a<caret>]", notExpected = listOf("yyyy_ab"))
    checkXyz()

    rInterop.executeCode("foo <- function() { xyz <<- 15; data.table(yyyy_ac = 1:3) }", true)
    checkCompletion("filter(foo(), yyyy_a<caret>)", notExpected = listOf("yyyy_ac"))
    checkXyz()

    rInterop.executeCode("`%foo%` <- function(x, y) { xyz <<- x + y; data.table(yyyy_ad = 2) }", true)
    checkCompletion("filter(20 %foo% 30, yyyy_a<caret>)", notExpected = listOf("yyyy_ad"))
    checkXyz()

    rInterop.executeCode("dt <- data.table(bac = 1:5)", true)
    checkCompletion("dt[, xyz := bac][ba<caret>]", expected = listOf("bac"), notExpected = listOf("xyz"))
    assertEquals("bac", rInterop.executeCode("cat(names(dt))", true).stdout)
    checkXyz()
  }

  fun testTransformExpression() {
    rInterop.executeCode("foo <- function(x) { x }", true)
    rInterop.executeCode("bar <- function(y) { y }", true)
    checkCompletion("table[yyyy_aaa == foo(5), .(yyyy_aad = c(bar(yyyy_aab)))][yyyy_a<caret>]",
                    initial = listOf("yyyy_aaa", "yyyy_aab"),
                    expected = listOf("yyyy_aaa", "yyyy_aab"),
                    notExpected = listOf("yyyy_aad"))
  }

  fun testRBind() {
    checkCompletion("rbind(table, yyyy_<caret>)",
                    initial = listOf("yyyy_aaa", "yyyy_aab"),
                    expected = listOf("\"yyyy_aaa\"", "\"yyyy_aab\""))

  }

  fun testDataFrame() {
    rInterop.executeCode("t <- data.frame(yyyy_aaa = NA, yyyy_aab = NA)", true)
    checkCompletion("as.data.table(t, key = foo(yyyy_<caret>))",
                    expected = listOf("\"yyyy_aaa\"", "\"yyyy_aab\""))
    checkCompletion("t[yyyy_<caret>]",
                    expected = emptyList())
  }

  fun testPipe() {
    checkCompletion("table %>% setkey(yyyy_a<caret>)",
      initial = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add", "yyyy_bbc"),
      expected = listOf("yyyy_aaa", "yyyy_aac", "yyyy_add"),
      notExpected = listOf("yyyy_bbc"))
  }

  fun testDataTableNotHaveTableArgument() {
    checkCompletion("data.table(table, yyyy_ax = 0, yyyy_ay = 1, yyyy_ac = yyyy_a<caret>)",
                    initial = listOf("yyyy_aa", "yyyy_ab"),
                    notExpected = listOf("yyyy_aa", "yyyy_ab", "yyyy_ax", "yyyy_ay"))
  }

  fun testColumnWithSpace() {
    rInterop.executeCode(
      """
        df <- data.table(c(1,2,3,4,5), c(2,3,4,5,6))
        colnames(df) <- c("some name", "some another name")
      """.trimIndent(),
      false
    )
    checkCompletion("setkey(df, som<caret>)", expected = listOf("\"some name\""))
  }

  fun testStaticColumnsFromDataTable() {
    checkStaticCompletion(
      """
        df <- data.table(my_column_1=c(1,2,3,4,5), my_column_2=c(2,3,4,5,6))
        setkey(df, my_colum<caret>)        
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_1", "my_column_2"),
      expectedToBeMissed = emptyList()
    )
  }

  fun testStaticColumnsFromSetnames() {
    checkStaticCompletion(
      """
        df <- data.table(my_column_1=c(1,2,3,4,5), my_column_2=c(2,3,4,5,6))
        df <- df %>% setnames(c("my_column_1", "my_column_2"), c("my_new_column1", "my_new_column2"))
        
        setkey(df, my_<caret>)
      """.trimIndent(),
      expectedToBePresent = listOf("my_new_column1", "my_new_column2"),
      expectedToBeMissed = listOf("my_column_1", "my_column_2")
    )
  }

  fun testNoVariableCompletionFromAssignmentInSubscription() {
    checkStaticCompletion(
      """
        dt <- dt[, my_column_c := 1 + 2]
        my_colum<caret>
      """.trimIndent(),
      expectedToBePresent = emptyList(),
      expectedToBeMissed = listOf("my_column_c")
    )
  }

  fun testStaticColumnsForSubscription() {
    checkStaticCompletion(
      """
        df <- df[ , c("my_new_column1", "my_new_column2")]
        df[, my_new_<caret>]
      """.trimIndent(),
      expectedToBePresent = listOf("my_new_column1"),
      expectedToBeMissed = emptyList()
    )
  }

  fun testStaticStringColumnsDefinedInSubscription() {
    checkStaticCompletion(
      """
        dt <- dt[, .("my_column_c", my_column_d := 3 + 4)]
        dt[,.(x=sum(my_col<caret>))]
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_c"),
      expectedToBeMissed = emptyList()
    )
  }

  fun testStaticColumnsDefinedInSubscription() {
    checkStaticCompletion(
      """
        dt <- dt[, .(my_column_c := 1 + 2, my_column_d := 3 + 4)]
        dt[,.(x=sum(my_col<caret>))]
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_c"),
      expectedToBeMissed = emptyList()
    )
  }

  fun testStaticColumnDeletedInSubscription() {
    checkStaticCompletion(
      """
        dt <- data.table(my_column_a = c(1, 2, 3), my_column_c = c(1,2,10))
        dt <- dt[, .(my_column_c := NULL, my_column_d := 3 + 4)]
        dt[,.(x=sum(my_col<caret>))]
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_a", "my_column_d"),
      expectedToBeMissed = listOf("my_column_c")
    )
  }

  fun testNoCompletionForVariableDefinedInDotSubscription() {
    checkStaticCompletion(
      """
        dt <- data.table(my_column_a = c(1, 2, 3), my_column_c = c(1,2,10))
        dt <- dt[, .(my_column_c := 1 + 4, my_column_d := 3 + 4)]
        dt[,.(x=sum(my_col<caret>))]
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_a", "my_column_c", "my_column_d"),
      expectedToBeMissed = listOf()
    )
  }

  fun testNoCompletionForVariableDefinedInCInSubscription() {
    checkStaticCompletion(
      """
        dt <- data.table(my_column_a = c(1, 2, 3), my_column_c = c(1,2,10))
        dt <- dt[, c(my_column_c := 1 + 2, my_column_d := 3 + 4)]
        dt[,.(x=sum(my_col<caret>))]
      """.trimIndent(),
      expectedToBePresent = listOf("my_column_a", "my_column_c", "my_column_d"),
      expectedToBeMissed = listOf()
    )
  }

  private fun checkCompletion(text: String,
                              expected: List<String> = emptyList(),
                              initial: List<String>? = null,
                              notExpected: List<String>? = null) {
    if (initial != null) {
      rInterop.executeCode("table <- data.table(${initial.joinToString(", ") { "$it = NA" }})", false)
    }
    myFixture.configureByText("a.R", text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic().toList()
    assertNotNull(result)

    val lookupStrings = result.map {
      val elementPresentation = LookupElementPresentation()
      it.renderElement(elementPresentation)
      elementPresentation.itemText!!
    }
    lookupStrings.filter { expected.contains(it) }.apply {
      assertEquals(distinct(), this)
    }

    assertContainsElements(lookupStrings, expected)
    if (notExpected != null) {
      assertDoesntContain(lookupStrings, notExpected)
    }
  }
}