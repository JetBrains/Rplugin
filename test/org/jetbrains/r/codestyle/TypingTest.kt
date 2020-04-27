/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.codestyle

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class TypingTest : RUsefulTestCase() {
  fun testAfterBraceInFunction() {
    doTest("""
      x <- function(a, b) {<caret>
      }
    """, """
      x <- function(a, b) {
        <caret>
      }
    """)
  }

  fun testAfterStatement() {
    doTest("""
      x <- 10500<caret>
      y <- function(a, b) {
      }
    """, """
      x <- 10500
      <caret>
      y <- function(a, b) {
      }
    """)
  }

  fun testEnterInParameters() {
    doTest("""
      y <- function(a,<caret>b) {
      }
    """, """
      y <- function(a,
                    <caret>b) {
      }
    """)
  }

  fun testEnterInArgumentsAfterCommaBeforeCloseParenthesis() {
    doTest("""
      y <- c(10, 20,<caret>)
    """, """
      y <- c(10, 20,
             <caret>)
    """)
  }

  fun testEnterAfterCommaBeforeCloseBracket() {
    doTest("""
      y <- df[num<0, <caret>]
    """, """
      y <- df[num<0,${' '}
              <caret>]
    """)
  }

  fun testEnterInParametersAfterCommaBeforeCloseParenthesis() {
    doTest("""
      yyy <- function (x,<caret>)
    """, """
      yyy <- function (x,
                       <caret>)
    """)
  }

  fun testEnterInParametersAfterCommaWithoutCloseParenthesis() {
    doTest("""
      yyy <- function (x,<caret>

      
    """, """
      yyy <- function (x,
                       <caret>
    
      
    """)
  }

  fun testEnterInArgumentsAfterCommaWithoutCloseParenthesis() {
    doTest("""
      y <- c(10, 20,<caret>

      
    """, """
      y <- c(10, 20,
             <caret>

      
    """)
  }

  // This behavior does not work in the end of file, see DS-208
  fun testEnterInExpression() {
    doTest("""
      someVariable <- 10500 +<caret>
      y <- 10
    """, """
      someVariable <- 10500 +
          <caret>
      y <- 10
    """)
  }

  fun testInTheEnd() {
    doTest("""
      x <- function(a, b) {
      }<caret>
    """, """
      x <- function(a, b) {
      }
      <caret>
    """)
  }

  fun testEnterInsideComment() {
    doTest("""
      # hello<caret> world
    """, """
      # hello
      # <caret>world
    """)
  }

  fun testAddParenthesis() {
    doTest("""
      foo<caret>
    """, """
      foo(<caret>)
    """, "(")
  }

  fun testAddParenthesisBeforeBinaryOperator() {
    doTest("""
      foo<caret>*2
    """, """
      foo(<caret>)*2
    """, "(")
  }


  fun testAddBracket() {
    doTest("""
      foo<caret>
    """, """
      foo[<caret>]
    """, "[")
  }

  fun testAddSecondBracket() {
    doTest("""
      foo[<caret>]
    """, """
      foo[[<caret>]]
    """, "[")
  }

  fun testDoNotAddParenthesis() {
    doTest("""
      foo<caret>10
    """, """
      foo(<caret>10
    """, "(")
  }

  fun testAddQuote() {
    doTest("""
      hello <- <caret>
    """, """
      hello <- "<caret>"
    """, "\"")
  }

  fun testAdd2Quotes() {
    doTest("""
      hello <- <caret>
    """, """
      hello <- ""<caret>
    """, "\"\"")
  }

  fun testRawString() {
    doTest("""
      hello <- r<caret>
    """, """
      hello <- r"(<caret>)"
    """, "\"")
  }

  fun testParenthesisInTheEndOfRFence() {
    doRmdTest("""
      ```{r}
      print<caret>
      ```
    """, """
      ```{r}
      print(<caret>)
      ```
    """, "(")
  }

  private fun doTest(@Language("R") fileText: String,
                     @Language("R") expected: String,
                     insert: String = "\n") {
    doGeneralTest(RFileType, fileText, expected, insert)
  }

  @Suppress("SameParameterValue")
  private fun doRmdTest(@Language("RMarkdown") fileText: String,
                        @Language("RMarkdown") expected: String,
                        insert: String) {
    doGeneralTest(RMarkdownFileType, fileText, expected, insert)
  }

  private fun doGeneralTest(fileType: FileType, fileText: String, expected: String, insert: String) {
    myFixture.configureByText(fileType, fileText.trimIndent())
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

  private fun doBackspaceTest(@Language("R") fileText: String,
                              @Language("R") expected: String) {
    myFixture.configureByText(RFileType, fileText.trimIndent())
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
    myFixture.checkResult(expected.trimIndent())
  }
}
