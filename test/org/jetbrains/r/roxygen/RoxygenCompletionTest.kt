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

  fun testLocalFun() {
    val text = """
      fun_bar <- function() {}
      
      #' [fun_<caret>]
      NULL
    """.trimIndent()
    doTest(text, "fun_bar")
    doApplyCompletionTest(text, "fun_bar", "#' [fun_bar()] <caret>")
  }

  fun testIdentifierPackage() {
    doTest("#' [dp<caret>]", "dplyr")
    doApplyCompletionTest("#' [dp<caret>]", "dplyr", "#' [dplyr::<caret>]")
  }

  fun testNamespaceAccess() {
    doTest("""
      fififi <- function() {}
      #' [dplyr::fi<caret>]
    """.trimIndent(), "filter", "filter_", "filter_all", "filter_at", "filter_if", "first", "db_query_fields")

    doWrongVariantsTest("""
      fififi <- function() {}
      #' [dplyr::fi<caret>]
    """.trimIndent(), "fififi")
    doApplyCompletionTest("#' [dplyr::fi<caret>]", "filter", "#' [dplyr::filter()] <caret>")
  }

  fun testFunFromPackage() {
    doTest("#' [roxy<caret>]", "roxygenize")
    doApplyCompletionTest("#' [roxy<caret>]", "roxygenize", "#' [roxygenize()] <caret>")
  }

  fun testVariable() {
    val text = """
      val_bar <- 42
      
      #' [val_<caret>]
      NULL
    """.trimIndent()
    doTest(text, "val_bar")
    doApplyCompletionTest(text, "val_bar", "#' [val_bar] <caret>")
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
