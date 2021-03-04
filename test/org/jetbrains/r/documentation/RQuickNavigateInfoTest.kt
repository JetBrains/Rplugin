/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import java.io.File
import java.nio.file.Path

private const val R_VERSION_LITERAL = "!!R_VERSION!!"

class RQuickNavigateInfoTest : RProcessHandlerBaseTestCase() {

  override fun getTestDataPath() = Path.of(super.getTestDataPath(), "documentation", "quickNavigate").toString()

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testLibraryFunction() = doTest("prin<caret>t()")

  fun testMultilineLibraryFunction() = doTest("read.<caret>table()")

  fun testPrimitiveFunction() = doTest("a <<caret>- 10")

  fun testDataFrame() = doTest("mt<caret>cars")

  fun testLibraryFunctionParameter() = doTest("read.csv(s<caret>ep = '.')")

  fun testUserFunction() = doTest("""
    my_function <- function(first, second = "Hello", third = 10) {}
    my_functi<caret>on()
  """.trimIndent())

  fun testUserFunctionParameter() = doTest("""
    my_function <- function(first, second = "Hello", third = 10) {}
    my_function(seco<caret>nd = "Welcome")
  """.trimIndent())

  fun testUserFunctionDeclaration() = doTest("my_func<caret>tion <- function(first, second = 'Hello', third = 10) {}")

  fun testUserVariableDeclaration() = doTest("my_va<caret>r <- 42")

  fun testUserParameterDeclaration() = doTest("my_var <- function(par<caret>am = 10) {}")

  fun testUserVariableUsage() = doTest("""
    my_var <- 42
    my_second_var = my_var + my<caret>_var
  """.trimIndent())

  fun testLibraryFunctionDarcula() = doTest("prin<caret>t()")

  fun testMultilineLibraryFunctionDarcula() = doTest("read.<caret>table()")

  fun testUserFunctionDarcula() = doTest("""
    my_function <- function(first, second = "Hello", third = 10) {}
    my_functi<caret>on()
  """.trimIndent())

  private fun doTest(code: String) {
    val isDarcula = getTestName(true).endsWith("Darcula")
    val editorColorsManager = EditorColorsManager.getInstance()
    editorColorsManager.globalScheme =
      if (isDarcula) editorColorsManager.getScheme("Darcula")
      else editorColorsManager.getScheme(EditorColorsScheme.DEFAULT_SCHEME_NAME)
    myFixture.configureByText("a.R", code)
    resolve().forEach {
      it.element?.containingFile?.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    val generated = getQuickNavigateText()
    val expectedFile = File(Path.of(testDataPath, getTestName(true) + ".html").toString())
    val rVersion = RInterpreterManager.getInstance(myFixture.project).interpreterOrNull!!.version.toString()
    val expectedText = expectedFile.readText().replace("\r?\n\\s*".toRegex(), "").replace(R_VERSION_LITERAL, rVersion).trim()
    assertEquals(expectedText, generated)
  }

  private fun getQuickNavigateText(): String {
    val info = GotoDeclarationAction().getCtrlMouseInfo(myFixture.editor, myFixture.file, myFixture.caretOffset)
    assertNotNull(info)
    val text = info!!.docInfo.text
    assertNotNull(text)
    return UIUtil.getHtmlBody(text!!)
  }
}
