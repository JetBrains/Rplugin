/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase

class RoxygenTypingTest : RUsefulTestCase() {

  // just to be sure
  fun testEnterBeforeComment() {
    doTest("""
      <caret>#' hello world
    """, """
      
      <caret>#' hello world
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

  fun testEnterInsideDocumentationCommentInFunction() {
    // it is strange situation but anyway...
    doTest("""
      func <- function() {
        #' hello<caret> world
      }
    """, """
      func <- function() {
        #' hello
        #' <caret>world
      }
    """)
  }

  fun testEnterAtTheEndOfDocumentationComment() {
    doTest("""
      #' hello world<caret>
      x <- 10
    """, """
      #' hello world
      #' <caret>
      x <- 10
    """)
  }

  fun testEnterAtTheEndOfFileAfterDocumentationComment() {
    doTest("""
      #' hello world<caret>
    """, """
      #' hello world
      #' <caret>
    """)
  }

  fun testEnterInsideDocumentationPrefix() {
    doTest("""
      #<caret>' hello world
    """, """
      #
      <caret>' hello world
    """)
  }

  fun testEnterAfterDocumentationPrefix() {
    doTest("""
      #'<caret> hello world
    """, """
      #'
      #' <caret>hello world
    """)
  }

  fun testBackspaceDocumentationCommentInTheEnd() {
    doBackspaceTest("""
      #' hello world
      #' <caret>
    """, """
      #' hello world
      <caret>
    """)
  }

  fun testBackspaceDocumentationCommentAndSpace() {
    doBackspaceTest("""
      #' hello world
      #' <caret>
      x <- 10
    """, """
      #' hello world
      <caret>
      x <- 10
    """)
  }

  fun testBackspaceDocumentationCommentWithoutSpace() {
    doBackspaceTest("""
      #' hello world
      #'<caret>
      x <- 10
    """, """
      #' hello world
      <caret>
      x <- 10
    """)
  }

  fun testBackspaceDocumentationCommentWithIndent() {
    doBackspaceTest("""
      y <- 1
        #' hello world
        #'<caret>
        x <- 10
    """, """
      y <- 1
        #' hello world
        <caret>
        x <- 10
    """)
  }

  fun testNormalBackspaceAfterCode() {
    doBackspaceTest("""
      y <- 1 #' <caret>
      x <- 10
    """, """
      y <- 1 #'<caret>
      x <- 10
    """)
  }

  fun testNormalBackspaceInsideComment() {
    doBackspaceTest("""
      #' hello world
      #' hello #'<caret>
      x <- 10
    """, """
      #' hello world
      #' hello #<caret>
      x <- 10
    """)
  }

  fun testClosingBracketNotInLink() {
    doTypeTest("""
      #' Just some brackets: <caret>]
    """, """
      #' Just some brackets: ]<caret>]
    """, "]")
  }

  fun testClosingBracketOvertype() {
    doTypeTest("""
      #' [foo<caret>]
    """, """
      #' [foo]<caret>
    """, "]")
  }

  fun testClosingBracketOvertypeEmpty() {
    doTypeTest("""
      #' [<caret>]
    """, """
      #' []<caret>
    """, "]")
  }

  fun testClosingBracketLinkWithTextOvertype() {
    doTypeTest("""
      #' [foo][bar<caret>]
    """, """
      #' [foo][bar]<caret>
    """, "]")
  }

  fun testClosingParenOvertype() {
    doTypeTest("""
      #' [foo](bar<caret>)
    """, """
      #' [foo](bar)<caret>
    """, ")")
  }

  fun testClosingParenOvertypeEmpty() {
    doTypeTest("""
      #' [foo](<caret>)
    """, """
      #' [foo]()<caret>
    """, ")")
  }

  fun testOpenBracket() {
    doTypeTest("""
      #' <caret>
    """, """
      #' [<caret>]
    """, "[")
  }

  fun testOpenBracketLinkWithText() {
    doTypeTest("""
      #' [foo]<caret>
    """, """
      #' [foo][<caret>]
    """, "[")
  }

  fun testOpenParen() {
    doTypeTest("""
      #' [foo]<caret>
    """, """
      #' [foo](<caret>)
    """, "(")
  }

  fun testOpenParenOutOfLink() {
    doTypeTest("""
      #' <caret>
    """, """
      #' (<caret>
    """, "(")
  }

  private fun doTest(@Language("R") fileText: String,
                     @Language("R") expected: String,
                     insert: String = "\n") {
    doGeneralTest(RFileType, fileText, expected, insert)
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

  private fun doTypeTest(@Language("R") fileText: String,
                         @Language("R") expected: String,
                         text: String) {
    myFixture.configureByText(RFileType, fileText.trimIndent())
    myFixture.type(text)
    myFixture.checkResult(expected.trimIndent())
  }
}
