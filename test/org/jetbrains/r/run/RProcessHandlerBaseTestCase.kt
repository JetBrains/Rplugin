/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RBooleanLiteral
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.run.debug.RLineBreakpointType

abstract class RProcessHandlerBaseTestCase : RUsefulTestCase() {
  protected lateinit var rInterop: RInterop

  override fun setUp() {
    super.setUp()
    rInterop = getRInterop(project)
    // we want be sure that the interpreter is initialized
    rInterop.executeCode("1")
  }

  override fun tearDown() {
    if (!Disposer.isDisposed(rInterop)) {
      Disposer.dispose(rInterop)
    }
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
            psi.argumentList.expressionList.filterIsInstance<RNamedArgument>().forEach {
              when (it.name) {
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
    const val DEFAULT_TIMEOUT = 20000

    private fun getRInterop(project: Project): RInterop {
      return RInteropUtil.runRWrapperAndInterop(project).blockingGet(DEFAULT_TIMEOUT)!!
    }
  }
}
