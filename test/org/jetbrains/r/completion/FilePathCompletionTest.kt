/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class FilePathCompletionTest : RLightCodeInsightFixtureTestCase() {
  fun testCompletion() {
    checkCompletion("\"<caret>\"",
                    listOf("aaa.txt", "bbb.txt", "ccc.kt"),
                    listOf("aaa.txt", "bbb.txt", "ccc.kt"))
  }

  fun testDirs() {
    checkCompletion("\"dir1/<caret>\"",
                    listOf("aa.txt", "bb.txt", "dir1/xy.java", "dir1/yz.kt", "dir1/zz/z.txt", "dir2/abc.R"),
                    listOf("xy.java", "yz.kt", "zz"))
  }

  fun testInSource() {
    checkCompletion("source(\"A<caret>\")",
                    listOf("aa.R", "A.R", "A1.R", "BA.R", "B.Rmd"),
                    listOf("A.R", "A1.R", "BA.R"))
  }

  fun testAbsolutePath() {
    val dir = "$testDataPath/filePathCompletion/"
    checkCompletion("\"$dir<caret>\"",
                    emptyList(),
                    listOf("absoluteFilePath.R"))
  }

  fun testInvalidPath() {
    checkCompletion("\"\\\\1 < \\\\2<caret>\"",
                    emptyList(),
                    emptyList())
  }

  private fun checkCompletion(text: String, files: List<String>, expected: List<String>) {
    myFixture.configureByText("currentFile.R", text)
    files.forEach { myFixture.addFileToProject(it, "") }
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }.filter { it != "currentFile.R" }
    TestCase.assertEquals(expected, lookupStrings)
  }
}