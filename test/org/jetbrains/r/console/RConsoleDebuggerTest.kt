/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import junit.framework.TestCase

class RConsoleDebuggerTest : RConsoleBaseTestCase() {
  fun testLogMessage() {
    val file = loadFileWithBreakpointsFromText("""
      1
      2
      3 # BREAKPOINT(suspend = FALSE, logMessage = TRUE)
      4
      5
    """.trimIndent(), "abc.R")
    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(false) {
      rInterop.replSourceFile(file, debug = true)
    }
    checkConsoleText("Breakpoint hit (abc.R:3)")
  }

  fun testLogStack() {
    val file = loadFileWithBreakpointsFromText("""
      foo <- function() {
        2 # BREAKPOINT(suspend = FALSE, logStack = TRUE)
      }
      bar <- function() {
        foo()
      }
      cat(bar())
    """.trimIndent(), "def.R")
    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(false) {
      rInterop.replSourceFile(file, debug = true)
    }
    checkConsoleText("1: foo (def.R:2)")
    checkConsoleText("2: bar (def.R:5)")
    checkConsoleText("3: cat")
    checkConsoleText("4: [global] (def.R:7)")
  }

  fun testSlaveBreakpoint() {
    val file = myFixture.configureByText("a.R", """
      for (i in 1:5) {
        if (i %% 2 == 0) {
          3
        }
        5
      }
    """.trimIndent()).virtualFile
    val master = addBreakpoint(file, 2)
    val slave = addBreakpoint(file, 4)
    val dependentBreakpointManager =
      (XDebuggerManager.getInstance(project).breakpointManager as XBreakpointManagerImpl).dependentBreakpointManager
    dependentBreakpointManager.setMasterBreakpoint(slave, master, false)
    master.suspendPolicy = SuspendPolicy.NONE

    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, debug = true) }
    TestCase.assertEquals("2", rInterop.executeCode("cat(i)").stdout)
    TestCase.assertEquals(4, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals("4", rInterop.executeCode("cat(i)").stdout)
    TestCase.assertEquals(4, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testSlaveBreakpointLeaveEnabled() {
    val file = myFixture.configureByText("a.R", """
      for (i in 1:5) {
        if (i == 4) {
          3
        }
        5
      }
    """.trimIndent()).virtualFile
    val master = addBreakpoint(file, 2)
    val slave = addBreakpoint(file, 4)
    val dependentBreakpointManager =
      (XDebuggerManager.getInstance(project).breakpointManager as XBreakpointManagerImpl).dependentBreakpointManager
    dependentBreakpointManager.setMasterBreakpoint(slave, master, true)
    master.suspendPolicy = SuspendPolicy.NONE

    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, debug = true) }
    TestCase.assertEquals("4", rInterop.executeCode("cat(i)").stdout)
    TestCase.assertEquals(4, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(true) { rInterop.debugCommandContinue() }
    TestCase.assertEquals("5", rInterop.executeCode("cat(i)").stdout)
    TestCase.assertEquals(4, rInterop.debugStack.lastOrNull()?.position?.line)
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testSlaveBreakpointConditionAndLog() {
    val file = myFixture.configureByText("a.R", """
      for (i in 1:5) {
        2
        3
      }
    """.trimIndent()).virtualFile
    val master = addBreakpoint(file, 1)
    val slave = addBreakpoint(file, 2)
    val dependentBreakpointManager =
      (XDebuggerManager.getInstance(project).breakpointManager as XBreakpointManagerImpl).dependentBreakpointManager
    dependentBreakpointManager.setMasterBreakpoint(slave, master, false)
    master.suspendPolicy = SuspendPolicy.NONE
    master.conditionExpression = XExpressionImpl.fromText("i %% 2 == 0")
    slave.suspendPolicy = SuspendPolicy.NONE
    slave.logExpressionObject = XExpressionImpl.fromText("paste0('<<', i, '>>')")

    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(false) { rInterop.replSourceFile(file, debug = true) }
    checkConsoleText("<<1>>", exclude = true)
    checkConsoleText("<<2>>")
    checkConsoleText("<<3>>", exclude = true)
    checkConsoleText("<<4>>")
    checkConsoleText("<<5>>", exclude = true)
  }

  fun testTemporaryBreakpoint() {
    val file = loadFileWithBreakpointsFromText("""
      for (i in 1:5) {
        2
        3 # BREAKPOINT(temporary = TRUE)
        4
      }
    """.trimIndent())
    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, debug = true) }
    TestCase.assertEquals("1", rInterop.executeCode("cat(i)").stdout)
    TestCase.assertEquals(2, rInterop.debugStack.last().position?.line)
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
  }

  fun testTemporaryBreakpointNoSuspend() {
    val file = loadFileWithBreakpointsFromText("""
      for (i in 1:3) {
        2
        3 # BREAKPOINT(suspend = FALSE, temporary = TRUE, evaluate = paste0('<<', i, '>>'))
        4
      }
    """.trimIndent())
    val helper = RConsoleDebuggerTestHelper(console)
    helper.invokeAndWait(false) { rInterop.replSourceFile(file, debug = true) }
    checkConsoleText("<<1>>")
    checkConsoleText("<<2>>", true)
    checkConsoleText("<<3>>", true)
  }

  fun testGlobalMuteBreakpointsAction() {
    val file = loadFileWithBreakpointsFromText("""
      1
      2 # BREAKPOINT
      3
    """.trimIndent())
    val action = ActionManager.getInstance().getAction("XDebugger.MuteBreakpoints") as ToggleAction
    val helper = RConsoleDebuggerTestHelper(console)

    action.setSelected(createAnActionEvent(), true)
    TestCase.assertTrue(console.debuggerPanel!!.breakpointsMuted)
    helper.invokeAndWait(false) { rInterop.replSourceFile(file, debug = true) }

    action.setSelected(createAnActionEvent(), false)
    TestCase.assertFalse(console.debuggerPanel!!.breakpointsMuted)
    helper.invokeAndWait(true) { rInterop.replSourceFile(file, debug = true) }
    helper.invokeAndWait(false) { rInterop.debugCommandContinue() }

    TestCase.assertFalse(action.isSelected(createAnActionEvent()))
    console.debuggerPanel!!.breakpointsMuted = true
    TestCase.assertTrue(action.isSelected(createAnActionEvent()))
  }

  private fun checkConsoleText(s: String, exclude: Boolean = false) {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    console.flushDeferredText()
    val text = console.editor!!.document.text
    if (exclude) {
      TestCase.assertTrue("Console contents:\n$text", s !in text)
    } else {
      TestCase.assertTrue("Console contents:\n$text", s in text)
    }
  }
}