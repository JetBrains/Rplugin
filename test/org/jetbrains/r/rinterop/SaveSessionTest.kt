/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.debugger.RDebuggerUtil
import com.intellij.r.psi.debugger.RSourcePosition
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterManager
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import org.jetbrains.r.run.debug.RLineBreakpointType

class SaveSessionTest : RUsefulTestCase() {
  private lateinit var interpreter: RInterpreter

  override fun setUp() {
    super.setUp()
    setupMockInterpreterManager()
    interpreter = RInterpreterManager.getInterpreterBlocking(project, RProcessHandlerBaseTestCase.DEFAULT_TIMEOUT)!!
    val workspaceFile = interpreter.createTempFileOnHost("a.RData")
    project.putUserData(RInteropUtil.WORKSPACE_FILE_FOR_TESTS, workspaceFile)
    runWriteAction {
      LocalFileSystem.getInstance().refreshAndFindFileByPath(workspaceFile)?.delete(this)
    }
  }

  override fun tearDown() {
    try {
      project.putUserData(RInteropUtil.WORKSPACE_FILE_FOR_TESTS, null)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun testSaveSession() {
    withRInterop { rInterop ->
      TestCase.assertTrue(rInterop.globalEnvRef.ls().all { it.startsWith('.') })
      rInterop.executeCode("a1 <- 'Hello'")
      rInterop.executeCode("a2 <- (1:5) ^ 2")
      rInterop.executeCode("foo <- function(a, b) return(a * 10 + b)")
    }
    withRInterop { rInterop ->
      TestCase.assertEquals(setOf("a1", "a2", "foo"), rInterop.globalEnvRef.ls().filter { !it.startsWith('.') }.toSet())
      TestCase.assertEquals("Hello", rInterop.executeCode("cat(a1)").stdout)
      TestCase.assertEquals("1 4 9 16 25", rInterop.executeCode("cat(a2)").stdout)
      TestCase.assertEquals("47", rInterop.executeCode("cat(foo(4, 7))").stdout)
    }
  }

  fun testSaveDebugSteps() {
    val file = myFixture.configureByText("abcd.R", """
      func <- function() {
        1
        2
        3
      }
    """.trimIndent()).virtualFile
    withRInterop { rInterop ->
      rInterop.replSourceFile(file).blockingGet(DEFAULT_TIMEOUT)
      rInterop.executeCode("func()")
      rInterop.executeCode("func()")
    }

    withRInterop { rInterop ->
      val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
      runWriteAction {
        XDebuggerManager.getInstance(project).breakpointManager.addLineBreakpoint(breakpointType, file.url, 2, null)
      }
      RDebuggerUtil.createBreakpointListener(rInterop)
      val helper = RDebuggerTestHelper(rInterop)
      helper.invokeAndWait(true) { rInterop.executeCodeAsync("func()", isRepl = true, debug = true) }
      TestCase.assertEquals(RSourcePosition(file, 2), rInterop.debugStack.last().position)
      helper.invokeAndWait(false) { rInterop.debugCommandContinue() }
    }
  }

  fun testSaveOnSigsegv() {
    withRInterop { rInterop ->
      TestCase.assertTrue(rInterop.globalEnvRef.ls().all { it.startsWith('.') })
      rInterop.executeCode("foo <- function(x) x^2")
      rInterop.executeCode("dd <- c(1, 4, 10)")
      rInterop.executeCodeAsync(".Call('.jetbrains_raiseSigsegv')").onError { }
      TestCase.assertTrue(rInterop.processHandler.waitFor(DEFAULT_TIMEOUT.toLong() * 100L))
    }
    withRInterop { rInterop ->
      TestCase.assertEquals(setOf("foo", "dd"), rInterop.globalEnvRef.ls().filter { !it.startsWith('.') }.toSet())
      TestCase.assertEquals("1 16 100", rInterop.executeCode("cat(foo(dd))").stdout)
    }
  }

  private inline fun withRInterop(f: (RInteropImpl) -> Unit) {
    val rInterop = RInteropUtil.runRWrapperAndInterop(interpreter).blockingGet(DEFAULT_TIMEOUT)!!
    try {
      f(rInterop)
    } finally {
      Disposer.dispose(rInterop)
    }
  }

  companion object {
    private const val DEFAULT_TIMEOUT = 20000
  }
}