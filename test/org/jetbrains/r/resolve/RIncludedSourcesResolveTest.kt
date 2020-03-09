/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
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
    val actualBeforeDeletion = getActualResults()
    assertEquals(expectedBeforeDeletion, actualBeforeDeletion)

    val fileForDeletion = files.find { it.name == "deleteThis.R" } ?: error("Missing file for deletion")
    runWriteAction { fileForDeletion.delete() }
    val expectedAfterDeletion = getExpectedResult("# after deletion", files.minus(fileForDeletion))
    val actualAfterDeletion = getActualResults()
    assertEquals(expectedAfterDeletion, actualAfterDeletion)
  }

  fun testFileCreation() {
    val fileName = "createThis.R"
    val files = getFiles { it != fileName }
    val expectedBeforeCreation = getExpectedResult("# before creation", files)
    val actualBeforeCreation = getActualResults()
    assertEquals(expectedBeforeCreation, actualBeforeCreation)

    val fileForCreation = File("$TEST_DATA_PATH/resolveInSource/fileCreation/createThis.R")
    if (!fileForCreation.exists()) error("Missing file for creation")

    val createdFile = myFixture.addFileToProject(fileName, fileForCreation.readText())
    val expectedAfterDeletion = getExpectedResult("# after creation", files.plus(createdFile))
    val actualAfterDeletion = getActualResults()
    assertEquals(expectedAfterDeletion, actualAfterDeletion)
  }

  private fun doTest() {
    val expected = getExpectedResult("# this")
    val actual = getActualResults()
    assertEquals(expected, actual)
  }

  override fun getFiles(editorFileName: String, filterPredicate: (String) -> Boolean): List<PsiFile> {
    val result = super.getFiles(editorFileName, filterPredicate)
    val dummy = myFixture.copyFileToProject("$fullTestDataPath/dummy.R", "dummy.R")
    return result.plus(PsiUtilCore.findFileSystemItem(project, dummy) as PsiFile)
  }
}