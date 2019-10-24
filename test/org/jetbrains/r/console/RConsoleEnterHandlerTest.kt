/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.editor.ex.EditorEx
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RConsoleEnterHandlerTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    resetEditor()
  }

  fun testStringQuotes() {
    checkFalse("'123")
    checkFalse("\"123")
    checkTrue("'123'")
    checkTrue("\"123\"")
  }

  fun testOperators() {
    checkFalse("a + ")
    checkFalse("""
      a +
      b +
      c +
    """.trimIndent())

    checkTrue("a + b")
    checkTrue("""
      a +
      b +
      c +
      d
    """.trimIndent())
  }

  fun testBrackets() {
    checkFalse("{")
    checkFalse("(")
    checkFalse("[")
    checkFalse("[[")
    checkFalse("[ ]")
    checkFalse("[[ ]]")


    checkTrue("{ }")
    checkTrue("( )")
    checkTrue("x[y]")
    checkTrue("x[[y]]")


    checkTrue("""{
      }""".trimMargin())
    checkTrue("""(
       )""")
    checkTrue("""x[
      y ]""".trimMargin())
    checkTrue("""x[[
      y]]""".trimMargin())
  }

  fun testFunction() {
    checkFalse("x <- function(x, y, z) {")
  }

  private fun checkFalse(text: String) = assertFalse(setText(text))
  private fun checkTrue(text: String) = assertTrue(setText(text))

  private fun resetEditor() {
    myFixture.configureByText(RFileType, "")
  }

  private fun setText(text: String): Boolean {
    myFixture.configureByText(RFileType, text)
    return RConsoleEnterHandler.handleEnterPressed(myFixture.editor as EditorEx)
  }
}