/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.codestyle

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase
import com.intellij.r.psi.rmarkdown.RMarkdownFileType

class RMarkdownFormatterTest : RUsefulTestCase() {
  fun testRMarkdownDefaultFenceFormatting() {
    // Do not use `print` method because it can be a keyword in some python versions and has different formatting style
    doTest("""
      ```{r}
      x <- function (a, b) {
      a+b
      }
      
      x (10, 2)
      ```
      
      ```{r}
      x (10, 2)
      
      ```

      ```{r}
        foo()
        bar()
      ```

    """, """
      ```{r}
      x <- function(a, b) {
        a + b
      }
      
      x(10, 2)
      ```
      
      ```{r}
      x(10, 2)
      
      ```
      
      ```{r}
      foo()
      bar()
      ```

    """)
  }

  @Suppress("SameParameterValue")
  private fun doTest(@Language("RMarkdown") fileText: String,
                     @Language("RMarkdown") expected: String) {
    myFixture.configureByText(RMarkdownFileType, fileText.trimIndent())
    WriteCommandAction.runWriteCommandAction(project) {
      val textRange = myFixture.file.textRange
      CodeStyleManager.getInstance(project).reformatText(myFixture.file, textRange.startOffset, textRange.endOffset)
    }
    myFixture.checkResult(expected.trimIndent())
  }
}
