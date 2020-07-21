/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parser

import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataPath
import com.jetbrains.python.psi.LanguageLevel
import org.jetbrains.r.RUsefulTestCase
import java.io.File

@TestDataPath("/testData/parser/rmd")
class RMarkdownParsingPythonTest : RUsefulTestCase() {

  override fun setUp() {
    super.setUp()
    // Any version can be used for this test but the psi-tree test answer may be different
    LanguageLevel.FORCE_LANGUAGE_LEVEL = LanguageLevel.PYTHON27
  }

  override fun getTestDataPath(): String = File(super.getTestDataPath(), "/parser/rmd/").path

  fun testDifferentCells() {
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
