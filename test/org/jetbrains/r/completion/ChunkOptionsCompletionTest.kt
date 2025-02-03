/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class ChunkOptionsCompletionTest : RProcessHandlerBaseTestCase() {
  fun testEcho() {
    doTest("```{r, e<caret>}```", "echo")
  }

  fun testFig() {
    val variants = arrayOf("align", "cap", "env", "ext", "height", "keep", "lp", "path", "pos", "retina", "scap", "show", "subcap", "width")
    doTest("```{r, fig.<caret>}```", *variants.map { "fig.$it" }.toTypedArray())
  }

  fun testMissingComma() {
    doTest("```{r, echo = TRUE fig}```", emptyCompletion = true)
  }

  fun testMissingCommaAfterR() {
    doTest("```{r ca<caret>}```", "cache")
  }

  fun testNotRChunk() {
    doTest("```{python fig<caret>}```", emptyCompletion = true)
    doTest("```{python, fig<caret>}```", emptyCompletion = true)
  }

  fun testUpperCaseR() {
    doTest("```{R fig<caret>}```", "fig.cap", "fig.env")
  }

  fun testApplyCompletion() {
    myFixture.configureByText("foo.Rmd", "")
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))

    doApplyCompletionTest("```{r,ca<caret>}```", "cache", "```{r, cache = <caret>}```", fileExtension = "Rmd")
    doApplyCompletionTest("```{r ca<caret>}```", "cache", "```{r cache = <caret>}```", fileExtension = "Rmd")
    doApplyCompletionTest("```{r \t  \t  e<caret>}```", "echo", "```{r \t  \t  echo = <caret>}```", fileExtension = "Rmd")
    doApplyCompletionTest("```{  \t \t r, \t  e<caret>}```", "echo", "```{  \t \t r, \t  echo = <caret>}```", fileExtension = "Rmd")
    doApplyCompletionTest("```{r, \te<caret>,smth = T}```", "echo", "```{r, \techo = <caret>,smth = T}```", fileExtension = "Rmd")
  }

  private fun doTest(text: String, vararg variants: String, emptyCompletion: Boolean = false) {
    myFixture.configureByText("foo.Rmd", text)
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic()
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    if (emptyCompletion) {
      assertEmpty(lookupStrings)
    }
    else {
      assertContainsOrdered(lookupStrings, *variants)
    }
  }
}