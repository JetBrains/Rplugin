/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import junit.framework.TestCase
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RBooleanLiteral
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import org.jetbrains.r.run.debug.RLineBreakpointType

abstract class RConsoleBaseTestCase : RProcessHandlerBaseTestCase() {
  protected lateinit var console: RConsoleView
    private set

  override fun setUp() {
    super.setUp()
    console = RConsoleView(rInterop, "dummyPath", project, "Test R Console")
    console.createDebuggerPanel()
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(console)
    // console is not running command, it just haven't received the first prompt from rwrapper
    var i = 0
    while (console.isRunningCommand && i++ < 100) { Thread.sleep(20) }
    check(!console.isRunningCommand) { "Cannot get prompt from rwrapper" }
  }

  override fun tearDown() {
    Disposer.dispose(console)
    RConsoleManager.getInstance(project).setCurrentConsoleForTests(null)
    super.tearDown()
  }

  protected fun addBreakpoint(file: VirtualFile, line: Int): XLineBreakpoint<*> {
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    return runWriteAction {
      XDebuggerManager.getInstance(project).breakpointManager.addLineBreakpoint(breakpointType, file.url, line, null)
    }
  }

  protected fun removeBreakpoint(breakpoint: XLineBreakpoint<*>) {
    runWriteAction {
      XDebuggerManager.getInstance(project).breakpointManager.removeBreakpoint(breakpoint)
    }
  }

  protected fun loadFileWithBreakpoints(path: String): VirtualFile {
    val file = myFixture.configureByFile(path)
    configureBreakpoints(file)
    return file.virtualFile
  }

  protected fun loadFileWithBreakpointsFromText(text: String, name: String = "a.R"): VirtualFile {
    val file = myFixture.configureByText(name, text)
    configureBreakpoints(file)
    return file.virtualFile
  }

  private fun configureBreakpoints(file: PsiFile) {
    file.text.lines().forEachIndexed { index, line ->
      if ("BREAKPOINT" in line) {
        var enabled = true
        var suspend = true
        var condition: String? = null
        var evaluate: String? = null
        runReadAction {
          val psi = RElementFactory.createRPsiElementFromText(project, "BREAKPOINT" + line.substringAfter("BREAKPOINT"))
          if (psi is RCallExpression) {
            psi.argumentList.expressionList.filterIsInstance<RAssignmentStatement>().forEach {
              when (it.assignee?.name) {
                "enabled" -> enabled = (it.assignedValue as RBooleanLiteral).isTrue
                "suspend" -> suspend = (it.assignedValue as RBooleanLiteral).isTrue
                "condition" -> condition = it.assignedValue?.text
                "evaluate" -> evaluate = it.assignedValue?.text
                else -> TestCase.fail("Invalid breakpoint description on line $index")
              }
            }
          }
        }
        val breakpoint = addBreakpoint(file.virtualFile, index)
        breakpoint.isEnabled = enabled
        breakpoint.suspendPolicy = if (suspend) SuspendPolicy.ALL else SuspendPolicy.NONE
        if (condition != null) {
          breakpoint.conditionExpression = XExpressionImpl.fromText(condition)
        }
        if (evaluate != null) {
          breakpoint.logExpressionObject = XExpressionImpl.fromText(evaluate)
        }
      }
    }
  }

  companion object {
    const val DEFAULT_TIMEOUT = 3000
  }
}