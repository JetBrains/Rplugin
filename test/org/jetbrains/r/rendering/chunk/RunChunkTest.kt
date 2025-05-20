/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.SyntaxTraverser
import junit.framework.TestCase
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes.FENCE_LANG
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.rinterop.RDebuggerTestHelper
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.visualization.inlays.InlayOutputData
import java.awt.Dimension

class RunChunkTest : RConsoleBaseTestCase() {

  override fun setUp() {
    super.setUp()
    RGraphicsUtils.createGraphicsDevice(rInterop, Dimension(640, 480), null)
  }

  fun testSimplePlot() {
    val runChunk = doRunChunk("""
      ```{r}
      plot(c(1,2),c(3,4))
      ```
    """.trimIndent())
    TestCase.assertTrue(runChunk.size == 1)
    TestCase.assertTrue(runChunk.all { it.type == "IMG" })
  }

  fun testDataOutput() {
    val runChunk = doRunChunk("""
      ```{r}
        cars
      ```
    """.trimIndent())
    assertTrue(runChunk.size == 1)
    assertTrue(runChunk.first().type == "TABLE")
  }

  fun testTextOutput() {
    val runChunk = doRunChunk("""
      ```{r}
        print("Hello world")
      ```
    """.trimIndent())
    assertTrue(runChunk.size == 1)
    assertTrue(runChunk.first().type == "Output")
  }

  fun testHtmlOutput() {
    rInterop.executeCode("library(plotly)", true)
    val result = doRunChunk("""
    ```{r}
    p <- plot_ly(
      type = 'scatterpolar',
      mode = 'lines'
    ) %>%
    add_trace(
      r = c(0, 1.5, 1.5, 0, 2.5, 2.5, 0),
      theta = c(0, 10, 25, 0, 205, 215, 0),
      fill = 'toself',
      fillcolor = '#709Bff',
      line = list(
        color = 'black'
      )
    ) %>%
    add_trace(
      r = c(0, 3.5, 3.5, 0),
      theta = c(0, 55, 75, 0),
      fill = 'toself',
      fillcolor = '#E4FF87',
      line = list(
        color = 'black'
      )
    ) %>%
    add_trace(
      r = c(0, 4.5, 4.5, 0, 4.5, 4.5, 0),
      theta = c(0, 100, 120, 0, 305, 320, 0),
      fill = 'toself',
      fillcolor = '#FFAA70',
      line = list(
        color = 'black'
      )
    ) %>%
    add_trace(
      r = c(0, 4, 4, 0),
      theta = c(0, 165, 195, 0),
      fill = 'toself',
      fillcolor = '#FFDF70',
      line = list(
        color = 'black'
      )
    ) %>%
    add_trace(
      r = c(0, 3, 3, 0),
      theta = c(0, 262.5, 277.5, 0),
      fill = 'toself',
      fillcolor = '#B6FFB4',
      line = list(
        color = 'black'
      )
    ) %>%
    layout(
      polar = list(
        radialaxis = list(
          visible = T,
          range = c(0,5)
        )
      ),
      showlegend = F
    )
    p
    ```
    """.trimIndent())
    assertTrue(
      "Got wrong number of URL results:\n${result.joinToString("\n") { "${it.type}: ${it.data}" }}",
      result.filter { it.type == "URL" }.size == 1
    )
  }

  fun testDebugChunk() {
    val helper = RDebuggerTestHelper(rInterop)
    RDebuggerUtil.createBreakpointListener(rInterop)

    val text = """
      Text
      Text
      ```{r}
      x <- 10
      x
      x <- 20 # BREAKPOINT
      x
      ```
    """.trimIndent()
    loadFileWithBreakpointsFromText(text, name = "foo.Rmd")
    val fenceLang = SyntaxTraverser.psiTraverser(myFixture.file).traverse().filter {
      it.node.elementType == FENCE_LANG &&
      it.nextSibling?.nextSibling?.node?.elementType == R_FENCE_ELEMENT_TYPE
    }.first()
    TestCase.assertNotNull(fenceLang)

    val promise = helper.invokeAndWait(true) {
      RunChunkHandler.getInstance(project).runHandlersAndExecuteChunkAsync(console, fenceLang!!, myFixture.editor as EditorEx, true)
    }
    TestCase.assertTrue(rInterop.isDebug)
    TestCase.assertEquals(listOf(5), rInterop.debugStack.map { it.position?.line })
    TestCase.assertEquals("10", rInterop.executeCode("cat(x)").stdout)

    helper.invokeAndWait(true) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals(listOf(6), rInterop.debugStack.map { it.position?.line })
    TestCase.assertEquals("20", rInterop.executeCode("cat(x)").stdout)

    helper.invokeAndWait(false) { rInterop.debugCommandStepOver() }
    promise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
  }

  fun testReadLockIsNotAcquired() {
    rInterop.executeCodeAsync("Sys.sleep(2)")
    val runChunk = doRunChunk("""
      ```{r}
        cars
      ```
    """.trimIndent())
    assertTrue(runChunk.size == 1)
    assertTrue(runChunk.first().type == "TABLE")
  }

  private fun doRunChunk(text: String, debug: Boolean = false): List<InlayOutputData> {
    loadFileWithBreakpointsFromText(text, name = "foo.Rmd")
    val fenceLang = SyntaxTraverser.psiTraverser(myFixture.file).traverse().filter {
      it.node.elementType == FENCE_LANG &&
      it.nextSibling?.nextSibling?.node?.elementType == R_FENCE_ELEMENT_TYPE
    }.first()
    TestCase.assertNotNull(fenceLang)
    val result = runAsync {
      runReadAction {
        RunChunkHandler.getInstance(project).runHandlersAndExecuteChunkAsync(console, fenceLang!!, myFixture.editor as EditorEx, debug)
      }
    }
    val firstPromise = result.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertNotNull(firstPromise)
    firstPromise?.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    return RMarkdownInlayDescriptor(myFixture.file).getInlayOutputs(fenceLang!!)
  }
}
