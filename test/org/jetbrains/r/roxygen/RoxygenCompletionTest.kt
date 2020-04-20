/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.roxygen

import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RoxygenCompletionTest : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testTagNames() {
    doTest("""
      #' @t<caret>
      NULL
    """.trimIndent(), "@title")

    doTest("""
      #' @par<caret>
      NULL
    """.trimIndent(), "@param")

    doWrongVariantsTest("""
      #' par<caret>
      NULL
    """.trimIndent(), "@param")
  }

  fun testParameters() {
    doTest("""
      #' @param xx<caret>
      fun <- function(xxxx, yyyy, xxxxyyyy)
    """.trimIndent(), "xxxx", "xxxxyyyy")

    doWrongVariantsTest("""
      #' xx<caret>
      fun <- function(xxxx, yyyy, xxxxyyyy)
    """.trimIndent(), "xxxx", "xxxxyyyy")
  }

  private fun doWrongVariantsTest(text: String, vararg variants: String) {
    val lookupStrings = getLookupStrings(text)
    assertDoesntContain(lookupStrings, *variants)
  }

  private fun doTest(text: String, vararg variants: String) {
    val lookupStrings = getLookupStrings(text)
    assertContainsOrdered(lookupStrings, *variants)
  }

  private fun getLookupStrings(text: String): List<String> {
    myFixture.configureByText("foo.R", text)
    val result = myFixture.completeBasic()
    assertNotNull(result)
    return result.map { it.lookupString }
  }
}
