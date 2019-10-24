/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.codestyle

import com.intellij.application.options.CodeStyle
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class RMarkdownTypingTest : RUsefulTestCase() {
  fun testFunctionInRFence() {
    doTest("""
      ```{r}
      x <- function (a, b) {<caret>
      }
      ```
    """, """
      ```{r}
      x <- function (a, b) {
        <caret>
      }
      ```
    """, "\n")
  }

  fun testEndInRFence() {
    doTest("""
      ```{r}
      x <- function (a, b) {
      }<caret>
      ```
    """, """
      ```{r}
      x <- function (a, b) {
      }
      <caret>
      ```
    """, "\n")
  }

  fun testInsideRFence() {
    doTest("""
      ```{r}
      x <- 10<caret>
      
      y <- function (a, b) {
      }
      ```
    """, """
      ```{r}
      x <- 10
      <caret>
      
      y <- function (a, b) {
      }
      ```
    """, "\n")
  }

  // There is a problem with last expression. It seems same nature as DS-208
  fun testAfterIncompleteExpressionInRFence() {
    doTest("""
      ```{r}
      x <- 10 +<caret>
      
      y <- function (a, b) {
      }
      ```
    """, """
      ```{r}
      x <- 10 +
          <caret>
      
      y <- function (a, b) {
      }
      ```
    """, "\n")
  }

  fun testStrangeIndentInTheStartInRFence() {
    doTest("""
      ```{r}
              x <- 10
      
      y <- 20<caret>
      
      z <- function (a, b) {
      }
      ```
    """, """
      ```{r}
              x <- 10
      
      y <- 20
      <caret>
      
      z <- function (a, b) {
      }
      ```
    """, "\n")
  }

  fun testFunctionInPythonFence() {
    doTest("""
      ```{python}
      def hello (n):<caret>
          foo (n)
      ```
    """, """
      ```{python}
      def hello (n):
          <caret>
          foo (n)
      ```
    """, "\n")
  }

  fun testNewFunctionInPythonFence() {
    doTest("""
      ```{python}
      def hello (n):<caret>
      
      foo()
      ```
    """, """
      ```{python}
      def hello (n):
          <caret>

      foo()
      ```
    """, "\n")
  }

  @Suppress("SameParameterValue")
  private fun doTest(@Language("RMarkdown") fileText: String,
                     @Language("RMarkdown") expected: String,
                     insert: String) {
    myFixture.configureByText(RMarkdownFileType, fileText.trimIndent())
    try {
      val settings: CodeStyleSettings = CodeStyle.getSettings(myFixture.file).clone()
      CodeStyleSettingsManager.getInstance(project).setTemporarySettings(settings)
      settings.getCommonSettings(RLanguage.INSTANCE).indentOptions?.CONTINUATION_INDENT_SIZE = 4
      settings.getCommonSettings(RLanguage.INSTANCE).indentOptions?.INDENT_SIZE = 2
      myFixture.type(insert)
    }
    finally {
      CodeStyleSettingsManager.getInstance(project).dropTemporarySettings()
    }
    myFixture.checkResult(expected.trimIndent())
  }
}
