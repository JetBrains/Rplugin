/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class PackageCompletionTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testLibrary() {
    doTest("library(<caret>)", "dplyr", "magrittr")
  }

  fun testRequire() {
    doTest("require(<caret>)", "dplyr", "magrittr")
  }

  fun testLibraryString() {
    doTest("library(\"<caret>\")", "dplyr", "magrittr")
  }

  fun testRequireString() {
    doTest("require(\"<caret>\")", "dplyr", "magrittr")
  }

  fun testIdentifierPacakge() {
    doTest("dp<caret>", "dplyr", "dpois")
  }

  fun testIdentifierPacakgeApply() {
    doApplyCompletionTest("dp<caret>", "dplyr", "dplyr::<caret>")
    doApplyCompletionTest("library(<caret>)", "dplyr", "library(dplyr<caret>)")
  }

  private fun doTest(text: String, vararg variants: String) {
    myFixture.configureByText("foo.R", text)
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    assertContainsElements(result.map { it.lookupString }, *variants)
  }
}