/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.mock.setupMockInterpreterManager
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

@Suppress("SpellCheckingInspection")
class StringCompletionTest : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
    myFixture.project.setupMockInterpreterManager()
  }

  fun testVector() {
    checkCompletion("a == \"X<caret>\"",
                    listOf("Xaaa", "Xbcd", "Xxyz"),
                    "a <- c(\"Xaaa\", \"Xbcd\", \"Xxyz\")")
  }

  fun testDplyr() {
    checkCompletion("a %>% filter(name == \"X<caret>\")",
                    listOf("Xaaa", "Xbcd", "Xxyz"),
                    "a <- tibble(name = c(\"Xaaa\", \"Xbcd\", \"Xxyz\"), i = c(1, 3, 7))")
  }

  fun testDplyrMemberAccess() {
    checkCompletion("a\$name == \"X<caret>\")",
                    listOf("Xaaa", "Xbcd", "Xxyz"),
                    "a <- tibble(name = c(\"Xaaa\", \"Xbcd\", \"Xxyz\"), i = c(1, 3, 7))")
  }

  fun testFactor() {
    checkCompletion("a == \"X<caret>\"",
                    listOf("Xaaa", "Xbcd", "Xxyz"),
                    "a <- factor(ordered(c(\"Xaaa\", \"Xbcd\", \"Xxyz\")))")
  }

  fun testFactorGgplot2() {
    rInterop.executeCode("library(ggplot2)", false)
    checkCompletion("diamonds %>% filter(cut == \"Go<caret>\")",
                    listOf("Good", "Very Good"))
  }

  private fun checkCompletion(text: String, expected: List<String>, initial: String? = null) {
    rInterop.executeCode("library(dplyr)", false)
    if (initial != null) {
      rInterop.executeCode(initial, false)
    }
    myFixture.configureByText("a.R", text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    TestCase.assertEquals(expected, lookupStrings)
  }
}