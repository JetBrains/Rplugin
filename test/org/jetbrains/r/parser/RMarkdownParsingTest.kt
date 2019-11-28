/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataPath
import com.jetbrains.python.psi.LanguageLevel
import org.jetbrains.r.RUsefulTestCase

private val DATA_PATH = System.getProperty("user.dir") + "/testData/parser/rmd/"

@TestDataPath("/testData/parser/rmd")
class RMarkdownParsingTest : RUsefulTestCase() {

  override fun setUp() {
    super.setUp()
    // Any version can be used for this test but the psi-tree test answer may be different
    LanguageLevel.FORCE_LANGUAGE_LEVEL = LanguageLevel.PYTHON27
  }

  override fun getTestDataPath(): String {
    return DATA_PATH
  }

  fun testSimple() {
    doTest()
  }

  fun testMulticell() {
    doTest()
  }

  fun testDifferentCells() {
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
    myFixture.configureByFile(testName + ".rmd")

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
