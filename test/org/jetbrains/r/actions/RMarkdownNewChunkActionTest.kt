/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import org.intellij.lang.annotations.Language

class RMarkdownNewChunkActionTest : EditorActionTest() {

  fun testInTheEmpty() {
    doTest("""
      <caret>
    """, """
      ```{r}
      <caret>
      ```
      
    """)
  }

  fun testInTextInTheLineEnd() {
    doTest("""
      text0
      text1<caret>
      text2
    """, """
      text0
      text1
      ```{r}
      <caret>
      ```
      text2
    """)
  }

  fun testInTextInTheLineMiddle() {
    doTest("""
      text0
      te<caret>xt1
      text2
    """, """
      text0
      text1
      ```{r}
      <caret>
      ```
      text2
    """)
  }

  fun testInTextInTheLineStart() {
    doTest("""
      text0
      <caret>text1
      text2
    """, """
      text0
      ```{r}
      <caret>
      ```
      text1
      text2
    """)
  }

  fun testAtEmptyLine() {
    doTest("""
      text1
      <caret>
      text2
    """, """
      text1
      ```{r}
      <caret>
      ```
      text2
    """)
  }

  fun testAt2EmptyLines() {
    doTest("""
      text1
      <caret>
      
      text2
    """, """
      text1
      ```{r}
      <caret>
      ```
      
      text2
    """)
  }

  fun testInTheEnd() {
    doTest("""
      text1<caret>
    """, """
      text1
      ```{r}
      <caret>
      ```
      
    """)
  }

  fun testInRFence() {
    doTest("""
      ```{r}
      x <- 10
      <caret>```
      
      text
    """, """
      ```{r}
      x <- 10
      ```
      ```{r}
      <caret>
      ```
      
      text
    """)
  }

  private fun doTest(@Language("RMarkdown") before: String,
                     @Language("RMarkdown") expected: String) {
    myFixture.configureByText("test.rmd", before.trimIndent())
    doActionTest(expected.trimIndent(), "RMarkdownNewChunk")
  }
}
