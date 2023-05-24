/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataPath
import org.jetbrains.r.RUsefulTestCase
import java.io.File

/** IMPORTANT: PSI has standard TokenType.WHITE_SPACE but lexical level use MARKDOWN_EOL. */
@TestDataPath("/testData/parser/rmd")
class RMarkdownParsingTest : RUsefulTestCase() {
  override fun getTestDataPath(): String = File(super.getTestDataPath(), "/parser/rmd/").path

  fun testSimple() {
    doTest()
  }

  fun testMulticell() {
    doTest()
  }

  fun testDifferentCellsWithParameters() {
    doTest()
  }

  fun testBackticksInFenceHeader() {
    doTest()
  }

  fun testRegression1() {
    doTest()
  }

  private fun doTest() {
    val testName = getTestName(true)
    myFixture.configureByFile("$testName.rmd")

    ParsingTestCase.doCheckResult(
      testDataPath,
      myFixture.file,
      true,
      testName,
      false,
      false,
      false)
  }
}
