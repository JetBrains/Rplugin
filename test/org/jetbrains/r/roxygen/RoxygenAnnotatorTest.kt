/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import java.nio.file.Paths

class RoxygenAnnotatorTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    runWriteAction { FileTypeManager.getInstance().associate(RoxygenFileType, ExtensionFileNameMatcher(RoxygenFileType.defaultExtension)) }
  }

  override fun getTestDataPath(): String = Paths.get(super.getTestDataPath(), "roxygen", "annotator").toString()

  fun testParams() = doTest()

  fun testMissingParamAfterComma() = doTest()

  fun testHelpPageLink() = doTest()

  fun testLinkDestination() = doTest()

  fun testAutolink() = doTest()

  private fun doTest() {
    val testName = getTestName(true)
    myFixture.testHighlighting(true, true, false, "$testName.roxygen")
  }
}