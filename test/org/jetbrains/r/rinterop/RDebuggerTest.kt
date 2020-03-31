/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.XDebuggerManager
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.RFileType
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RDebuggerTest : RProcessHandlerBaseTestCase() {
  private lateinit var helper: RDebuggerTestHelper

  override fun setUp() {
    super.setUp()
    val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
    runWriteAction {
      breakpointManager.allBreakpoints.forEach { breakpointManager.removeBreakpoint(it) }
    }
    helper = RDebuggerTestHelper(rInterop)
    RDebuggerUtil.createBreakpointListener(rInterop)
  }

  fun testBreakpoints() {
    val file = loadFileWithBreakpointsFromText("""
      foo = function(x, y, z) {
        a = x + y + z
        return(a) # BREAKPOINT
      }
      bar = function(aa, bb) {
        return(foo(aa, bb, aa + bb))
      }
      print(123) # BREAKPOINT
      bar(50, 600)
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(null), rInterop.debugStack.map { it.functionName })
    TestCase.assertEquals(listOf(RSourcePosition(file, 7)), rInterop.debugStack.map { it.position })

    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(listOf(null, "bar", "foo"), rInterop.debugStack.map { it.functionName })
    TestCase.assertEquals(listOf(
      RSourcePosition(file, 8),
      RSourcePosition(file, 5),
      RSourcePosition(file, 2)
    ), rInterop.debugStack.map { it.position })

    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testChangingBreakpoints() {
    val file = myFixture.configureByText("a.R", """
      fff = function() {
        1
        2
        3
      }
      fff()
      fff()
    """.trimIndent()).virtualFile

    val breakpoint1 = addBreakpoint(file, 1)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(5, 1), rInterop.debugStack.map { it.position?.line })

    val breakpoint2 = addBreakpoint(file, 3)
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(listOf(5, 3), rInterop.debugStack.map { it.position?.line })

    addBreakpoint(file, 2)
    removeBreakpoint(breakpoint1)
    removeBreakpoint(breakpoint2)
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(listOf(6, 2), rInterop.debugStack.map { it.position?.line })

    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testBreakpointInLibraryFunction() {
    val file = loadFileWithBreakpointsFromText("""
      ggg = function() {
        return(5) # BREAKPOINT
      }
      cat(ggg())
    """.trimIndent())
    val catFile = RRef.expressionRef("cat", rInterop).functionSourcePosition()!!.file
    TestCase.assertTrue(RSourceFileManager.isTemporary(catFile))
    TestCase.assertFalse(RSourceFileManager.isTemporary(file))
    addBreakpoint(catFile, 1)

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(file, catFile), rInterop.debugStack.map { it.position?.file })
    TestCase.assertEquals(listOf(null, "cat"), rInterop.debugStack.map { it.functionName })

    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(listOf(file, catFile, file), rInterop.debugStack.map { it.position?.file })
    TestCase.assertEquals(listOf(null, "cat", "ggg"), rInterop.debugStack.map { it.functionName })

    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testMuteBreakpoints() {
    val file = loadFileWithBreakpointsFromText("""
      0
      1 # BREAKPOINT
      2
      3 # BREAKPOINT
      4
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(1, rInterop.debugStack.lastOrNull()?.position?.line)
    rInterop.debugMuteBreakpoints(true)
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }

    helper.invokeAndWait(false) { rInterop.replSourceFile(file, true) }

    rInterop.debugMuteBreakpoints(false)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(1, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(3, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testStackVariables() {
    val file = loadFileWithBreakpointsFromText("""
      fff = function() {
        var3 = 0
        return(1) # BREAKPOINT
      }
      
      ggg = function(x) {
        var2 = 1
        return(x + 1)
      }
      
      hhh = function() {
        var1 = 2
        return(ggg(fff() / 2))
      }
      
      var0 = 3
      hhh()
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(4, rInterop.debugStack.size)
    TestCase.assertTrue("var0" in rInterop.debugStack[0].environment.ls())
    TestCase.assertTrue("var1" in rInterop.debugStack[1].environment.ls())
    TestCase.assertTrue("var2" in rInterop.debugStack[2].environment.ls())
    TestCase.assertTrue("var3" in rInterop.debugStack[3].environment.ls())

    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testStepOver() {
    val file = loadFileWithBreakpointsFromText("""
      x <- 10
      x <- 20 # BREAKPOINT
      x <- 30
      x <- 40
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(1, rInterop.debugStack.lastOrNull()?.position?.line)
    TestCase.assertEquals("10", rInterop.executeCode("cat(x)").stdout)

    helper.invokeAndWait(true) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals(2, rInterop.debugStack.lastOrNull()?.position?.line)
    TestCase.assertEquals("20", rInterop.executeCode("cat(x)").stdout)

    rInterop.debugMuteBreakpoints(true)
    helper.invokeAndWait(true) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals(3, rInterop.debugStack.lastOrNull()?.position?.line)
    TestCase.assertEquals("30", rInterop.executeCode("cat(x)").stdout)

    helper.invokeAndWait(false) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals("40", rInterop.executeCode("cat(x)").stdout)
  }

  fun testFunctionSteps() {
    val file = loadFileWithBreakpointsFromText("""
      ff <- function() {
        1
        2
        3
      }
      hh <- function() {
        6
        ff() # 7
        8
      } # 9
      { # 10
        hh() # 11 # BREAKPOINT
        hh() # 12
        13
      }
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(11), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(listOf(11, 6), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals(listOf(11, 7), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(listOf(11, 7, 1), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepOut() }
    TestCase.assertEquals(listOf(11, 8), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepOver() }
    TestCase.assertEquals(listOf(12), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(listOf(12, 6), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(true) { rInterop.debugCommandStepOut() }
    TestCase.assertEquals(listOf(13), rInterop.debugStack.map { it.position!!.line })

    helper.invokeAndWait(false) { rInterop.debugCommandStepInto() }
  }

  fun testForceStepInto() {
    val file = loadFileWithBreakpointsFromText("""
      f <- function(x) {
        return(x + 10)
      }
      cat(f(2)) # BREAKPOINT
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(null), rInterop.debugStack.map { it.functionName })
    TestCase.assertEquals(3, rInterop.debugStack.last().position!!.line )

    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(listOf(null, "cat", "f"), rInterop.debugStack.map { it.functionName })
    TestCase.assertEquals(1, rInterop.debugStack.last().position!!.line )

    helper.invokeAndWait(false) { rInterop.debugCommandStepOver() }

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(null), rInterop.debugStack.map { it.functionName })
    TestCase.assertEquals(3, rInterop.debugStack.last().position!!.line )

    helper.invokeAndWait(true) { rInterop.debugCommandForceStepInto() }
    TestCase.assertEquals(listOf(null, "cat"), rInterop.debugStack.map { it.functionName })

    helper.invokeAndWait(false) { rInterop.debugCommandStepOut() }
  }

  fun testConditionalBreakpoint() {
    val file = loadFileWithBreakpointsFromText("""
      foo <- function(x) {
        return(x * 2) # BREAKPOINT(condition = x > 10)
      }
      bar <- function() {
        foo(1)  # 4 # BREAKPOINT
        foo(11) # 5
        foo(2)  # 6 # BREAKPOINT(condition = DO_ERROR)
      }
      bar() # 8
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(8, 4), rInterop.debugStack.map { it.position?.line })
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals(listOf(8, 5, 1), rInterop.debugStack.map { it.position?.line })
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testEvaluatingBreakpoint() {
    val file = loadFileWithBreakpointsFromText("""
      x <- "A"
      x <- "BB" # BREAKPOINT(suspend = FALSE, evaluate = (newVar <- paste0("[", x, "]")))
      x <- "CCC" # BREAKPOINT(suspend = FALSE, evaluate = (newVar <- "no"), condition = x == "A")
    """.trimIndent())

    helper.invokeAndWait(false) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals("[A]", rInterop.executeCode("cat(newVar)").stdout)
  }

  fun testEvaluateAndPrintBreakpoint() {
    val buf = StringBuilder()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onText(text: String, type: ProcessOutputType) {
        if (type == ProcessOutputType.STDERR) {
          buf.append(text)
        }
      }
    })
    val file = loadFileWithBreakpointsFromText("""
      x <- "A"
      x <- "BB" # BREAKPOINT(suspend = FALSE, evaluate = x)
      x <- "CCC" # BREAKPOINT(suspend = FALSE, evaluate = x, condition = x == "A")
    """.trimIndent())

    helper.invokeAndWait(false) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals("[1] \"A\"", buf.toString().trim())
  }

  fun testRunToPosition() {
    val file = loadFileWithBreakpointsFromText("""
      f <- function() {
        1
        2
        3
      }
      5 # BREAKPOINT
      f() # 6
      g <- function() {
        8 # BREAKPOINT
        f()
      }
      g() # 11
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(5), rInterop.debugStack.map { it.position?.line })

    helper.invokeAndWait(true) { rInterop.debugCommandRunToPosition(RSourcePosition(file, 2)) }
    TestCase.assertEquals(listOf(6, 2), rInterop.debugStack.map { it.position?.line })

    helper.invokeAndWait(true) { rInterop.debugCommandRunToPosition(RSourcePosition(file, 1)) }
    TestCase.assertEquals(listOf(11, 8), rInterop.debugStack.map { it.position?.line })

    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testPauseAndStop() {
    val file = loadFileWithBreakpointsFromText("""
      aa <- 3
      while (TRUE) {
        aa <- aa + 1
      }
    """.trimIndent())

    val breakpoint = addBreakpoint(file, 2)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(2), rInterop.debugStack.map { it.position?.line })
    TestCase.assertEquals("3", rInterop.executeCode("cat(aa)").stdout)

    removeBreakpoint(breakpoint)
    helper.invokeAndWait(true) {
      rInterop.debugCommandContinue()
      Thread.sleep(300)
      rInterop.debugCommandPause()
    }
    TestCase.assertTrue(rInterop.executeCode("cat(aa)").stdout.toInt() > 1000)
    helper.invokeAndWait(false) {
      rInterop.debugCommandStop()
    }
  }

  fun testStopRunning() {
    val file = loadFileWithBreakpointsFromText("""
      aa <- 3 # BREAKPOINT
      while (TRUE) {
        aa <- aa + 1
      }
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    helper.invokeAndWait(false) {
      rInterop.debugCommandContinue()
      Thread.sleep(300)
      rInterop.debugCommandStop()
    }
    TestCase.assertTrue(rInterop.executeCode("cat(aa)").stdout.toInt() > 1000)
  }

  fun testExceptionHandler() {
    myFixture.configureByText(RFileType, """
      f <- function(xx, yy, zz) {
        aaa <- xx * yy * zz
        stop("Abcd 1234") # 2
      }
      g <- function(abc) {
        return(f(abc, 10, 20)) # 5
      }
      g(15) # 7
    """.trimIndent())

    val promise = AsyncPromise<String>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onException(exception: RExceptionInfo) {
        promise.setResult(exception.message)
      }
    })
    helper.invokeAndWait(false) { rInterop.replSourceFile(myFixture.file.virtualFile) }
    TestCase.assertEquals("Abcd 1234", promise.blockingGet(DEFAULT_TIMEOUT))
    TestCase.assertEquals(listOf(null, "g", "f", "stop"), rInterop.lastErrorStack.map { it.functionName })
    TestCase.assertEquals(listOf(7, 5, 2, 0), rInterop.lastErrorStack.map { it.position?.line })
    TestCase.assertEquals(setOf("f", "g"), rInterop.lastErrorStack[0].environment.ls().filter { !it.startsWith('.') }.toSet())
    TestCase.assertEquals(setOf("abc"), rInterop.lastErrorStack[1].environment.ls().toSet())
    TestCase.assertEquals(setOf("xx", "yy", "zz", "aaa"), rInterop.lastErrorStack[2].environment.ls().toSet())
  }

  fun testGlobalStepInto() {
    val file = loadFileWithBreakpointsFromText("""
      f <- function() {
        return(123)
      }
      print(1) # BREAKPOINT
      x <- 10
      print(f())
      x <- 20
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(3, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(4, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(5, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(1, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandStepInto() }
    TestCase.assertEquals(6, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(false) { rInterop.debugCommandStepInto() }
  }

  fun testDebugPrint() {
    val file = loadFileWithBreakpointsFromText("""
      print.abacaba <- function(x) {
        print(123) # BREAKPOINT
      }
      yy <- "42"
      class(yy) <- "abacaba"
      yy
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(5, 0, 1), rInterop.debugStack.map { it.position!!.line })
    TestCase.assertEquals(listOf(null, "print", "print.abacaba"), rInterop.debugStack.map { it.functionName })
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testNoStepIntoNamespaceAccess() {
    val file = loadFileWithBreakpointsFromText("""
      a <- base::stderr() # BREAKPOINT
      b <- tools:::httpdPort
    """.trimIndent())

    helper.invokeAndWait(true) { rInterop.replSourceFile(file, true) }
    TestCase.assertEquals(listOf(0), rInterop.debugStack.map { it.position!!.line })
    helper.invokeAndWait(true) { rInterop.debugCommandForceStepInto() }
    TestCase.assertEquals(listOf(1), rInterop.debugStack.map { it.position!!.line })
    helper.invokeAndWait(false) { rInterop.debugCommandForceStepInto() }
  }

  fun testSource() {
    val secondFile = FileUtil.createTempFile("b", ".R", true)
    val firstFile = loadFileWithBreakpointsFromText("""
      foo <- function() {
        1
        2 # BREAKPOINT
        3
      }
      5
      source("${StringUtil.escapeStringCharacters(secondFile.absolutePath)}")
      7
    """.trimIndent())
    secondFile.writeText("""
      0
      1
      2
      foo()
      4
    """.trimIndent())
    val secondVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(secondFile)

    helper.invokeAndWait(true) { rInterop.replSourceFile(firstFile, true) }
    val stack = rInterop.debugStack.mapNotNull { it.position }
    TestCase.assertEquals(1, stack.count { it.file == firstFile && it.line == 6 })
    TestCase.assertEquals(1, stack.count { it.file == secondVirtualFile && it.line == 3 })
    TestCase.assertEquals(1, stack.count { it.file == firstFile && it.line == 2 })
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }
}
