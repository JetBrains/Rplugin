/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints

import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.rmarkdown.RMarkdownFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RReturnHintsTest : RLightCodeInsightFixtureTestCase() {

  fun testSingleReturn() {
    doRFileTest("""
      foo <- function(a, b, c) { 
        l <- length(b)
        a + l + c <${e}foo>
      }
    """.trimIndent())
  }

  fun testImplicitTwoReturn() {
    doRFileTest("""
      foo <- function(a, b, c) { 
        if (T) {
          42 <${e}foo>
        } 
        else 43 <${e}foo>
      }
    """.trimIndent())
  }

  fun testImplicitNullReturn() {
    doRFileTest("""
      foo <- function(a, b, c) { 
        if (T) {
          42 <${e}foo>
        } <${n}foo>
      }
    """.trimIndent())
  }

  fun testReturnCallResult() {
    doRFileTest("""
      foo <- function() {
        if (T) {
          return(42) <${e}foo>
        }
        if (F) {
          15 + 16 <${e}foo>
        }
        else 43 <${e}foo>
      }
    """.trimIndent())
  }

  fun testForExpression() {
    doRFileTest("""
      foo <- function() {
        if (T) {
          return(42) <${e}foo>
        }
        b = 0
        for (a in 1:10) {
          b = b + 10
        } <${n}foo>
      }
    """.trimIndent())
  }

  fun testLambda() {
    doRFileTest("""
      function() {
        if (F) {
          42 <${e}lambda>
        } <${n}lambda>
      }
    """.trimIndent())
  }

  fun testInnerFunction() {
    doRFileTest("""
      outer <- function(a) {
        b <- a + 10
        inner <- function(c) {
          val <- 42
          42 * c <${e}inner>
        }
        
        inner(b) <${e}outer>
      }
    """.trimIndent())
  }

  fun testFunWithError() {
    doRFileTest("""
      foo <- function() {
        val <- 42
        10 + <${e}foo>
      }
    """.trimIndent())
  }

  fun testTwoExtensionOnOneLine() {
    doRFileTest("""
      foo <- function(a) {
        if (a) 42 <${e}foo> <${n}foo>
      }
    """.trimIndent())
  }

  fun testNotBlockBody() {
    doRFileTest("""
      foo <- function(a, b) a + b 
    """.trimIndent())
  }

  fun testEmptyBlockBody() {
    doRFileTest("""
      foo <- function(a, b) {}
    """.trimIndent())
  }

  fun testSingleExpressionBlockBody() {
    doRFileTest("""
      foo <- function(a, b) {
        42 + a - bar(b, a)
      }
      
      bar <- function(a, b) {
        if (a > b) {
          a + b <${e}bar>
        }
        else {
          a - b <${e}bar>
        } 
      }
      
      baz <- function() {
        for (i in 1:10) {
          a <- i + 10
        } <${n}baz>
      }
    """.trimIndent())
  }

  fun testRmd() {
    doRMarkdownFileTest("""
      ```{r}
      foo <- function(a, b, c) { 
        l <- length(b)
        a + l + c <${e}foo>
      }
      
      foo <- function(a, b, c) { 
        if (T) {
          42 <${e}foo>
        } <${n}foo>
      }
      
      outer <- function(a) {
        b <- a + 10
        inner <- function(c) {
          val <- 42
          42 * c <${e}inner>
        }
              
        inner(b) <${e}outer>
      }
      ```
    """.trimIndent())
  }

  fun testMultiBlocksRmd() {
    doRMarkdownFileTest("""
      ```{r}
      foo <- function(a) {
        val <- 42
        val <${e}foo>
      }
      ```
      
      ```{python}
      def foo(a):
        val = 42
        return 42 + 42
      ```
      
      ```{r}
      foo1 <- function(a, b, c) {
        val <- 42
        a + b + c <${e}foo1>
      }
      ```
    """.trimIndent())
  }

  fun testDisableImplicitNullResult() {
    doRFileTest("""
      foo <- function() {
        if (T) {
          42 <${e}foo>
        }
      }
    """.trimIndent(), implicitNullEnabled = false)
  }

  fun testAtLeastTwoReturnStatements() {
    doRFileTest("""
      foo1 <- function() {
        42 + 43
      }
      
      foo2 <- function() {
        if (T) {
          42 <${e}foo2>
        }
        else {
          43 <${e}foo2>
        }
      }
      
      foo3 <- function() {
        if (T) {} <${n}foo3>
      }
    """.trimIndent(), returnCnt = 2)
  }

  fun testAtLEastTwoReturnWithDisableImplicitNull() {
    doRFileTest("""
      foo <- function() {
        if (T) {
          42
        }
      }
      
      foo <- function() {
        if (T) {
          42 <${e}foo>
        }
        else {
          43 <${e}foo>
        }
      }
    """.trimIndent(), 2, false)
  }

  fun testNamedArgumentFunction() {
    doRFileTest("""
      tryCatch(expr, error = function(e) {
        b <- 10
        b + 8 <${e}error>
      })
    """.trimIndent())
  }

  fun testMemberExpression() {
    doRFileTest("""
      foo${"$"}bar <- function(x, y) {
        a <- 42 + 43
        42 <${e}foo${"$"}bar>
      }
    """.trimIndent())

    doRFileTest("""
      foo${"$"}bar() <- function(x, y) {
        a <- 42 + 43
        42 <${e}lambda>
      }
    """.trimIndent())

    doRFileTest("""
      foo()${"$"}bar <- function(x, y) {
        a <- 42 + 43
        42 <${e}lambda>
      }
    """.trimIndent())
  }

  private fun changeSettings(returnCnt: Int, implicitNullEnabled: Boolean) {
    val inlayHintsSettings = InlayHintsSettings.instance()
    val settings = inlayHintsSettings.findSettings(RReturnHintInlayProvider.settingsKey, RLanguage.INSTANCE) {
      val settings = RReturnHintInlayProvider.Settings()
      inlayHintsSettings.storeSettings(RReturnHintInlayProvider.settingsKey, RLanguage.INSTANCE, settings)
      settings
    }
    settings.differentReturnExpressions = returnCnt
    settings.showImplicitReturn = implicitNullEnabled
  }

  private fun doRFileTest(text: String, returnCnt: Int = 1, implicitNullEnabled: Boolean = true) {
    changeSettings(returnCnt, implicitNullEnabled)
    doTest(text, RFileType.DOT_R_EXTENSION.drop(1))
  }

  private fun doRMarkdownFileTest(text: String, returnCnt: Int = 1, implicitNullEnabled: Boolean = true) {
    changeSettings(returnCnt, implicitNullEnabled)
    doTest(text, RMarkdownFileType.defaultExtension)
  }

  private fun doTest(text: String, fileExtension: String) {
    val realText = text.replace(Regex(" <.*?>"), "")
    myFixture.configureByText("a.$fileExtension", realText)

    myFixture.doHighlighting()

    val lines = realText.split("\n").toMutableList()
    for (i in lines.indices) {
      (myFixture.editor as EditorImpl).processLineExtensions(i) { extensionInfo ->
        lines[i] = lines[i] + extensionInfo.text.let {
          if (it.isBlank()) it else "<${it.replace("<", "&lt;").replace(">", "&rt;")}>"
        }
        true
      }
    }

    assertEquals(text, lines.joinToString("\n"))
  }

  companion object {
    private val e = RBundle.message("inlay.hints.function.return.expression.explicit.prefix")
    private val n = RBundle.message("inlay.hints.function.return.expression.implicit.prefix").replace("<", "&lt;").replace(">", "&rt;")
  }
}