/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import java.nio.file.Paths

class RAnnotatorTest : RLightCodeInsightFixtureTestCase() {

  override fun getTestDataPath(): String = Paths.get(super.getTestDataPath(), "annotator").toString()

  fun testLocalVariableHighlighting() = doTest()

  fun testFunctionDeclaration() = doTest()

  fun testFunctionCall() = doTest()

  fun testFor() = doTest()

  fun testClosure() = doTest()

  fun testNamedParameters() = doTest()

  fun testAtOperator() = doTest()

  fun testListSubsetOperator() = doTest()

  fun testDots() = doTest()

  private fun doTest() {
    val testName = getTestName(true)
    myFixture.testHighlighting(true, true, false, testName + ".R")
  }

}