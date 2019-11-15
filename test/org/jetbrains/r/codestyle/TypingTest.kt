/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.codestyle

import com.intellij.application.options.CodeStyle
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase

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

  // just to be sure
  fun testEnterBeforeComment() {
    doTest("""
      <caret>#' hello world
    """, """
      
      <caret>#' hello world
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

  fun testEnterInsideDocumentationComment() {
    doTest("""
      #' hello<caret> world
    """, """
      #' hello
      #' <caret>world
    """)
  }

  private fun doTest(@Language("R") fileText: String,
                     @Language("R") expected: String,
                     insert: String = "\n") {
    myFixture.configureByText(RFileType, fileText.trimIndent())
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
