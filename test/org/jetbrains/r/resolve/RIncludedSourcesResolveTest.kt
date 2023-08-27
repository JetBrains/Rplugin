/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.util.SystemInfo
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import junit.framework.TestCase
import org.apache.commons.lang.StringUtils
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.psi.api.RFile
import java.io.File

class RIncludedSourcesResolveTest : RResolveFromFilesTestCase("resolveInSource") {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testAmbiguousInclude() = doTest()

  fun testAmbiguousIncludeInDeepSource() = doTest()

  fun testAmbiguousIncludeWithoutElseBranch() = doTest()

  fun testDeclarationOverridesSourceInSource() = doTest()

  fun testFilepathNamedArgument() = doTest()

  fun testFilepathNotFirstArgument() = doTest()

  fun testLocalOverridesSourceAfter() = doTest()

  fun testLocalOverridesSourceBefore() = doTest()

  fun testQualifiedSourceCall() = doTest()

  // This case is not supported. The test is needed to make sure that there are no errors or stack overflow. Behavior is undefined
  fun testRecursiveFunction() = doTest()

  // This case is not supported. The test is needed to make sure that there are no errors or stack overflow. Behavior is undefined
  fun testMutuallyRecursiveFunctions() = doTest()

  fun testOverridesSourceFunction() = doTest()

  fun testSerialIncludes() = doTest()

  fun testSimple() = doTest()

  fun testSourceFromDeepFunction() = doTest()

  fun testSourceFromFunction() = doTest()

  fun testSourceFromSource() = doTest()

  fun testSourceInForAfter() = doTest()

  fun testSourceInForBefore() = doTest()

  fun testSourceOverridesDeclarationInSource() = doTest()

  fun testSourceOverridesLocalAfter() = doTest()

  fun testSourceOverridesLocalBefore() = doTest()

  fun testInvalidPath() = doTest()

  fun testInfixOperator() = doTest()

  fun testFileDeletion() {
    val files = getFiles()
    val expectedBeforeDeletion = getExpectedResult("# before deletion", files)
    val actualBeforeDeletion = getActualResultsWithTimeLimit()
    assertEquals(expectedBeforeDeletion, actualBeforeDeletion)

    val fileForDeletion = files.find { it.name == "deleteThis.R" } ?: error("Missing file for deletion")
    runWriteAction { fileForDeletion.delete() }
    val expectedAfterDeletion = getExpectedResult("# after deletion", files.minus(fileForDeletion))
    val actualAfterDeletion = getActualResultsWithTimeLimit()
    assertEquals(expectedAfterDeletion, actualAfterDeletion)
  }

  fun testFileCreation() {
    val fileName = "createThis.R"
    val files = getFiles { it != fileName }
    val expectedBeforeCreation = getExpectedResult("# before creation", files)
    val actualBeforeCreation = getActualResultsWithTimeLimit()
    assertEquals(expectedBeforeCreation, actualBeforeCreation)

    val fileForCreation = File("$TEST_DATA_PATH/resolveInSource/fileCreation/createThis.R")
    if (!fileForCreation.exists()) error("Missing file for creation")

    val createdFile = myFixture.addFileToProject(fileName, fileForCreation.readText())
    val expectedAfterDeletion = getExpectedResult("# after creation", files.plus(createdFile))
    val actualAfterDeletion = getActualResultsWithTimeLimit()
    assertEquals(expectedAfterDeletion, actualAfterDeletion)
  }

  // Checking that functions that don't include any sources don't participate in the dependency graph
  fun testCallsWithoutIncludedSources() {
    val mainText = """
      bar <- function() {}
      foo <- function() {}
      source("B.R")
      ${StringUtils.repeat("foo()\n", 25000)}
      ba<caret>r()
    """.trimIndent()
    myFixture.configureByText("main.R", mainText).also { file ->
      // Pre-build a dependency graph, since it's slow on such large example
      (file as RFile).includedSources
    }
    val bFile = myFixture.addFileToProject("B.R", "bar <- 42")
    val expected = setOf(PsiElementWrapper(bFile.firstChild))
    val actual = getActualResultsWithTimeLimit()
    assertEquals(expected, actual)
  }

  fun testLotsDuplicates() {
    val mainText = """
      bar <- function() {}
      foo <- function() {
        ${StringUtils.repeat("source('C.R')\nsource('D.R')\n", 100)}
      }
      
      source("B.R")
      ${StringUtils.repeat("foo()\n", 1000)}
      ba<caret>r()
    """.trimIndent()
    myFixture.configureByText("main.R", mainText)
    val bFile = myFixture.addFileToProject("B.R", "bar <- 42")
    val expected = setOf(PsiElementWrapper(bFile.firstChild))
    val actual = getActualResultsWithTimeLimit()
    assertEquals(expected, actual)
  }

  fun testUIFreezes() {
    val mainText = buildString {
      appendLine("bar <- function() {}")
      for (i in 0..1000) {
        appendLine("foo$i <- function {")
        for (j in 0..100) {
          appendLine("  source('file${i * 100 + j}')")
        }
        appendLine("}\n")
      }

      appendLine("source('B.R')")
      for (i in 0..1000) {
        appendLine("foo$i()")
      }
      appendLine("ba<caret>r()")
    }
    myFixture.configureByText("main.R", mainText).also { (it as RFile).includedSources }

    var delay = 10L
    while (delay < 1500) {
      runAsync { ProgressManager.getInstance().runInReadActionWithWriteActionPriority({ getActualResults() }, ProgressIndicatorBase()) }

      Thread.sleep(delay)
      var writeActionTime = System.currentTimeMillis()
      runWriteAction {
        writeActionTime = System.currentTimeMillis() - writeActionTime
      }
      // No UI Freezes
      TestCase.assertTrue("Timeout: $writeActionTime > 300\nDelay: $delay", writeActionTime < 300)
      delay *= 2
    }
  }

  private fun doTest() {
    val expected = getExpectedResult("# this")
    val actual = getActualResultsWithTimeLimit()
    assertEquals(expected, actual)
  }

  private fun getActualResultsWithTimeLimit(): Set<PsiElementWrapper> {
    val result = runAsync { runReadAction { getActualResults() } }.blockingGet(if (SystemInfo.isWindows) 800 else 500)
    assertNotNull("Resolve is too long", result)
    return result!!
  }

  override fun getFiles(editorFileName: String, filterPredicate: (String) -> Boolean): List<PsiFile> {
    val result = super.getFiles(editorFileName, filterPredicate)
    val dummy = myFixture.copyFileToProject("$fullTestDataPath/dummy.R", "dummy.R")
    return result.plus(PsiUtilCore.findFileSystemItem(project, dummy) as PsiFile)
  }
}