/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.psi.api.RExpression
import java.io.File

class RIncludedSourcesResolveTest : RLightCodeInsightFixtureTestCase() {

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

  private fun doTest() {
    val filenames = File(TEST_DATA_PATH + "/resolveInSource/" + getTestName(true))
                      .listFiles()?.map { it.path }?.sortedByDescending { it.contains("main") }?.plus(listOf<String>(TEST_DATA_PATH + "/resolveInSource/dummy.R"))
                    ?: error("No files")
    val files = myFixture.configureByFiles(*filenames.toTypedArray())
    val actual = resolve().mapNotNull { PsiElementWrapper(it.element ?: return@mapNotNull null) }.toSet()
    val expected = collectExpectedResult(files).map { PsiElementWrapper(it) }.toSet()
    assertEquals(expected, actual)
  }

  private class PsiElementWrapper(private val elem: PsiElement) {
    override fun equals(other: Any?): Boolean {
      if (other !is PsiElementWrapper) return false
      return elem == other.elem
    }

    override fun hashCode(): Int {
      return elem.hashCode()
    }

    override fun toString(): String {
      return "${(elem as? PsiNamedElement)?.name ?: elem.text} from `${elem.containingFile.name}`"
    }
  }

  private fun collectExpectedResult(files: Array<PsiFile>): List<PsiElement> {
    return files.flatMap { file ->
      val text = file.viewProvider.getPsi(RLanguage.INSTANCE).text
      Regex("# this").findAll(text).map { it.range.first }.mapNotNull {
        PsiTreeUtil.getPrevSiblingOfType(file.findElementAt(it), RExpression::class.java)
      }.toList()
    }
  }
}