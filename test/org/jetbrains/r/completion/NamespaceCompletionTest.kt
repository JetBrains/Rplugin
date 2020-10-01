/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import org.intellij.lang.annotations.Language
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class NamespaceCompletionTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testDplyr() {
    doTest("""
      dplyr::fi<caret>
    """.trimIndent(), "filter", "filter_", "filter_all", "filter_at", "filter_if", "first", "db_query_fields")
  }

  fun testDplyrDataset() {
    completeSingle("dplyr::sto<caret>", "dplyr::storms")
  }

  fun testDplyrInternal() {
    completeSingle("dplyr:::abort_case_when_formu<caret>", "dplyr:::abort_case_when_formula(<caret>)")
  }

  private fun doTest(text: String, vararg variants: String) {
    myFixture.configureByText("foo.R", text)
    val result = myFixture.completeBasic()
    assertNotNull(result)
    assertOrderedEquals(result.map { it.lookupString }, *variants)
  }

  private fun completeSingle(@Language("R") text: String, @Language("R") expected: String) {
    myFixture.configureByText("foo.R", text)
    val completion = myFixture.completeBasic()
    LOG.warn("")
    assertNull("Got ${completion?.joinToString(", ") { it.lookupString }} instead of null", completion) // means a single variant completed
    myFixture.checkResult(expected)
  }
}