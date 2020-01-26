/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.openapi.application.runReadAction
import com.intellij.testFramework.fixtures.CompletionAutoPopupTester
import icons.org.jetbrains.r.psi.TableInfo
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RValueSimple
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.settings.REditorSettings

class AutoPopupTest : RLightCodeInsightFixtureTestCase() {
  private lateinit var myTester: CompletionAutoPopupTester

  override fun setUp() {
    super.setUp()
    myTester = CompletionAutoPopupTester(myFixture)
    addLibraries()
  }

  fun testPackageAccess() {
    doTest("""
    """.trimIndent(), "dplyr::", true)
  }

  fun testMemberAccess() {
    doTest("""
      foo<caret>
    """.trimIndent(), "$", true, DummyRuntimeInfo())
  }

  fun testMemberAccessAfterVariable() {
    try {
      assertTrue("Check default value", REditorSettings.disableCompletionAutoPopupForShortPrefix)
      REditorSettings.disableCompletionAutoPopupForShortPrefix = false
      myFixture.configureByText(RFileType, """
            x<caret> 
          """.trimIndent())
      runReadAction {
        myFixture.file.addRuntimeInfo(DummyRuntimeInfo())
      }
      myTester.typeWithPauses("x")
      assertNotNull(myTester.getLookup())
      myTester.typeWithPauses("$")
      assertNotNull(myTester.getLookup())
    }
    finally {
      REditorSettings.disableCompletionAutoPopupForShortPrefix = true
    }
  }

  fun testShortPrefix() {
    assertTrue("Check default value", REditorSettings.disableCompletionAutoPopupForShortPrefix)
    myFixture.configureByText(RFileType, """
          x<caret> 
        """.trimIndent())
    runReadAction {
      myFixture.file.addRuntimeInfo(DummyRuntimeInfo())
    }
    myTester.typeWithPauses("x")
    assertNull(myTester.getLookup())
    myTester.typeWithPauses("$")
    assertNotNull(myTester.getLookup())
  }

  fun testMemberAccessInFunction() {
    doTest("""
      x <- function() {
        boo()
        foo<caret>
      }
    """.trimIndent(), "$", true, DummyRuntimeInfo())
  }

  fun testMemberAccessNotLast() {
    doTest("""
      foo<caret>
      boo()
    """.trimIndent(), "$", true, DummyRuntimeInfo())
  }

  fun testFilePath() {
    listOf("foo/bar", "foo/baz").forEach { myFixture.addFileToProject(it, "") }
    doTest("""
      "foo<caret>"
    """.trimIndent(), "/", true)
  }

  fun testPackageAccessRMarkdown() {
    doTest("""
      ```{r}
        <caret>
      ```
    """.trimIndent(), "dplyr::", true)
  }

  fun testMemberAccessRMarkdown() {
    doTest("""
      ```{r}
      foo<caret>
      ```
    """.trimIndent(), "$", true, DummyRuntimeInfo())
  }

  fun testPackageIncomplete() {
    doTest("""
    """.trimIndent(), "dplyr:", false)
  }

  fun testPackageIncompleteRMarkdown() {
    doTest("""
      ```{r}
      <caret>
      ```
    """.trimIndent(), "dplyr:", false)
  }


  fun testFilePathRMarkdown() {
    listOf("foo/bar", "foo/baz").forEach { myFixture.addFileToProject(it, "") }
    doTest("""
      "foo<caret>"
    """.trimIndent(), "/", true)
  }

  fun doTest(sourceText: String, typedText: String, showPopup: Boolean, rConsoleRuntimeInfo: RConsoleRuntimeInfo? = null) {
    myFixture.configureByText(if (sourceText.contains("```{r}")) RMarkdownFileType else RFileType, sourceText)
    runReadAction {
      rConsoleRuntimeInfo?.let { myFixture.file.findElementAt(myFixture.caretOffset - 1)?.containingFile?.addRuntimeInfo(it) }
    }
    myTester.typeWithPauses(typedText)
    if (showPopup) {
      assertNotNull(myTester.getLookup())
    }
    else {
      assertNull(myTester.getLookup())
    }
  }

  override fun runInDispatchThread(): Boolean {
    return false
  }

  override fun invokeTestRunnable(runnable: Runnable) {
    myTester.runWithAutoPopupEnabled(runnable)
  }

  private class DummyRuntimeInfo : RConsoleRuntimeInfo {
    override val variables = mapOf(
      "xx" to RValueSimple(""),
      "xxx" to RValueSimple("")
    )
    override val loadedPackages: Map<String, Int> = emptyMap()
    override val rMarkdownChunkOptions: List<String> = emptyList()
    override val workingDir: String
      get() = throw NotImplementedError()

    override fun loadPackage(name: String) {
      throw NotImplementedError()
    }

    override fun loadDistinctStrings(expression: String): List<String> {
      throw NotImplementedError()
    }

    override fun loadObjectNames(expression: String): List<String> {
      return listOf("foo", "bar")
    }

    override fun loadAllNamedArguments(expression: String): List<String> {
      throw NotImplementedError()
    }

    override fun getFormalArguments(expression: String): List<String> {
      throw NotImplementedError()
    }

    override fun loadTableColumns(expression: String): TableInfo {
      throw NotImplementedError()
    }


    override val rInterop: RInterop
      get() = throw NotImplementedError()
  }
}