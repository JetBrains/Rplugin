// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.xdebugger.impl.XSourcePositionImpl
import icons.org.jetbrains.r.RBundle
import junit.framework.TestCase

class RDebuggerTest : RConsoleBaseTestCase() {
  fun testBreakpointAndSteps() {
    val file = loadFileWithBreakpointsFromText("""x <- 10
      |x <- 20 # BREAKPOINT
      |x <- 30
      |x <- 40""".trimMargin())

    TestCase.assertFalse(console.debugger.isEnabled)
    console.debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertTrue(console.debugger.isEnabled)
    TestCase.assertEquals("10", rInterop.executeCode("cat(x)", true).stdout)
    console.debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals("20", rInterop.executeCode("cat(x)", true).stdout)
    console.debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals("30", rInterop.executeCode("cat(x)", true).stdout)
    console.debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals("40", rInterop.executeCode("cat(x)", true).stdout)
    TestCase.assertFalse(console.debugger.isEnabled)
  }

  fun testLanguage() {
    val file = loadFileWithBreakpointsFromText("x = 1 # BREAKPOINT")
    rInterop.executeCode("Sys.setenv(LANGUAGE = 'ru_RU.UTF-8')", true)
    TestCase.assertEquals("ru_RU.UTF-8", rInterop.executeCode("cat(Sys.getenv('LANGUAGE'))", true).stdout)
    console.debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertEquals("en_US.UTF-8", rInterop.executeCode("cat(Sys.getenv('LANGUAGE'))", true).stdout)
    console.debugger.resume().blockingGet(5000)
    TestCase.assertEquals("ru_RU.UTF-8", rInterop.executeCode("cat(Sys.getenv('LANGUAGE'))", true).stdout)
  }

  fun testStackTrace() {
    val file = loadFileWithBreakpoints("debugger/stackTrace.R")
    console.debugger.executeDebugSource(file).blockingGet(5000)
    var stack = console.debugger.stack
    TestCase.assertEquals(listOf("foo", "bar", "baz", GLOBAL), stack.map { it.functionName })
    TestCase.assertEquals(List(4) { file }, stack.map { it.sourcePosition?.file })
    TestCase.assertEquals(listOf(2, 9, 15, 18), stack.map { it.sourcePosition?.line })
    console.debugger.resume().blockingGet(5000)
    TestCase.assertFalse(console.debugger.isEnabled)
    stack = console.debugger.stack
    TestCase.assertEquals(listOf(GLOBAL), stack.map { it.functionName })
    TestCase.assertNull(stack.first().sourcePosition)
  }

  fun testFunctionSteps() {
    val file = loadFileWithBreakpoints("debugger/functionSteps.R")
    val debugger = console.debugger

    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertEquals(listOf(5), debugger.stack.map { it.sourcePosition?.line })
    debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals(listOf(6), debugger.stack.map { it.sourcePosition?.line })
    debugger.stepInto().blockingGet(5000)
    TestCase.assertEquals(listOf(0, 6), debugger.stack.map { it.sourcePosition?.line })
    debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals(listOf(1, 6), debugger.stack.map { it.sourcePosition?.line })
    debugger.stepOut().blockingGet(5000)
    TestCase.assertEquals(listOf(7), debugger.stack.map { it.sourcePosition?.line })
    debugger.runToCursor(XSourcePositionImpl.create(file, 2)).blockingGet(5000)
    TestCase.assertEquals(listOf(2, 7), debugger.stack.map { it.sourcePosition?.line })
    debugger.resume().blockingGet(5000)
  }

  fun testConditionalBreakpoint() {
    val file = loadFileWithBreakpoints("debugger/conditionalBreakpoint.R")
    val debugger = console.debugger

    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertEquals("bar", debugger.stack.first().functionName)
    debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals("bar", debugger.stack.first().functionName)
    debugger.stepOver().blockingGet(5000)
    TestCase.assertEquals("foo", debugger.stack.first().functionName)
    debugger.stepOut().blockingGet(5000)
    TestCase.assertEquals("bar", debugger.stack.first().functionName)
    debugger.stepOver().blockingGet(5000)
    TestCase.assertFalse(debugger.isEnabled)
  }

  fun testEvaluatingBreakpoint() {
    val file = loadFileWithBreakpoints("debugger/evaluatingBreakpoint.R")
    val debugger = console.debugger

    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertFalse(debugger.isEnabled)
    TestCase.assertEquals("[A]", rInterop.executeCode("cat(newVar)", true).stdout)
  }

  fun testBreakpointsInRuntime() {
    val file = loadFileWithBreakpoints("debugger/breakpointsInRuntime.R")
    val debugger = console.debugger

    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertEquals(1, debugger.stack.first().sourcePosition?.line)
    val breakpoint = addBreakpoint(file, 3)
    debugger.resume().blockingGet(5000)
    TestCase.assertEquals(3, debugger.stack.first().sourcePosition?.line)
    debugger.resume().blockingGet(5000)
    TestCase.assertEquals(1, debugger.stack.first().sourcePosition?.line)
    removeBreakpoint(breakpoint)
    debugger.resume().blockingGet(5000)
    TestCase.assertEquals(1, debugger.stack.first().sourcePosition?.line)
    debugger.resume().blockingGet(5000)
    TestCase.assertFalse(debugger.isEnabled)
  }

  fun testForceStepInto() {
    val file = loadFileWithBreakpointsFromText("""
      cat(1) # BREAKPOINT
      cat(2)
      cat(3)
    """.trimIndent(), name = "a.R")
    val debugger = console.debugger

    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertEquals("a.R", debugger.stack.first().sourcePosition?.file?.name)
    TestCase.assertEquals(0, debugger.stack.first().sourcePosition?.line)
    debugger.stepInto().blockingGet(5000)
    TestCase.assertEquals("a.R", debugger.stack.first().sourcePosition?.file?.name)
    TestCase.assertEquals(1, debugger.stack.first().sourcePosition?.line)
    debugger.forceStepInto().blockingGet(5000)
    TestCase.assertEquals("cat", debugger.stack.first().sourcePosition?.file?.name)
    TestCase.assertEquals("cat", debugger.stack.first().functionName)
    debugger.stepOut().blockingGet(5000)
    TestCase.assertEquals("a.R", debugger.stack.first().sourcePosition?.file?.name)
    TestCase.assertEquals(2, debugger.stack.first().sourcePosition?.line)
    debugger.stepInto().blockingGet(5000)
    TestCase.assertFalse(debugger.isEnabled)
  }

  fun testBackslashes() {
    val file = loadFileWithBreakpointsFromText("""
      abc = "\\"
      xyz = all(grepl('^(\\d*%|-)${'$'}', 'foobarbaz'))
    """.trimIndent(), name = "a.R")
    val debugger = console.debugger
    debugger.executeDebugSource(file).blockingGet(5000)
    TestCase.assertFalse(debugger.isEnabled)
    rInterop.executeCode("cat(abc)").let {
      TestCase.assertEquals("", it.stderr)
      TestCase.assertEquals("\\", it.stdout)
    }
    rInterop.executeCode("cat(xyz)").let {
      TestCase.assertEquals("", it.stderr)
      TestCase.assertEquals("FALSE", it.stdout)
    }
  }

  companion object {
    private val GLOBAL = RBundle.message("debugger.global.stack.frame")
  }
}