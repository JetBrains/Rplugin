/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

class RPackageProjectResolveTest : RResolveFromFilesTestCase("resolveInPackageProject") {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testProjectPackageAlsoInLibrary() = doTest()

  fun testTestFunFromTestFile() = doTest()

  fun testTestFunFromSrcFile() = doTest()

  private fun doTest() {
    myFixture.copyFileToProject("$fullTestDataPath/DESCRIPTION", "DESCRIPTION") // Fake dplyr
    val expected = getExpectedResult("# this")
    val actual = getActualResults()
    assertEquals(expected, actual)
  }
}