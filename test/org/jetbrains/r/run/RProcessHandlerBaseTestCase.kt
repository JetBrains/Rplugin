/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import junit.framework.TestCase
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterStateImpl
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RBooleanLiteral
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.run.debug.RLineBreakpointType
import java.nio.file.Files

abstract class RProcessHandlerBaseTestCase : RUsefulTestCase() {
  protected lateinit var rInterop: RInterop
  protected open val customDeadline: Long? = null
  protected lateinit var interpreter: RInterpreter

  open fun alwaysCreateNewInterop() = false

  override fun setUp() {
    super.setUp()
    Files.createDirectories(project.stateStore.projectBasePath)
    project.putUserData(RInterop.DEADLINE_TEST_KEY, customDeadline)
    setupMockInterpreterManager()
    setupMockInterpreterStateManager()
    interpreter = RInterpreterManager.getInterpreterAsync(project).blockingGet(DEFAULT_TIMEOUT)!!
    rInterop = if (alwaysCreateNewInterop()) createRInterop(interpreter) else getRInterop(interpreter)
    // we want be sure that the interpreter is initialized
    rInterop.executeCode("1")
  }

  override fun tearDown() {
    try {
      project.putUserData(RInterop.DEADLINE_TEST_KEY, null)
      if (this::rInterop.isInitialized && !Disposer.isDisposed(rInterop)) {
        runAsync {
          Disposer.dispose(rInterop)
        }
      }
      XDebuggerManager.getInstance(project).breakpointManager.let { manager ->
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
        manager.getBreakpoints(breakpointType).forEach { manager.removeBreakpoint(it) }
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  protected fun addBreakpoint(file: VirtualFile, line: Int): XLineBreakpoint<*> {
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    return runWriteAction {
      XDebuggerManager.getInstance(project).breakpointManager.addLineBreakpoint(breakpointType, file.url, line, null)
    }
  }

  protected fun removeBreakpoint(breakpoint: XLineBreakpoint<*>) {
    XDebuggerManager.getInstance(project).breakpointManager.removeBreakpoint(breakpoint)
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
        var logMessage: Boolean = false
        var logStack: Boolean = false
        var temporary = false
        runReadAction {
          val psi = RElementFactory.createRPsiElementFromText(project, "BREAKPOINT" + line.substringAfter("BREAKPOINT"))
          if (psi is RCallExpression) {
            psi.argumentList.expressionList.filterIsInstance<RNamedArgument>().forEach {
              when (it.name) {
                "enabled" -> enabled = (it.assignedValue as RBooleanLiteral).isTrue
                "suspend" -> suspend = (it.assignedValue as RBooleanLiteral).isTrue
                "condition" -> condition = it.assignedValue?.text
                "evaluate" -> evaluate = it.assignedValue?.text
                "logMessage" -> logMessage = (it.assignedValue as RBooleanLiteral).isTrue
                "logStack" -> logStack = (it.assignedValue as RBooleanLiteral).isTrue
                "temporary" -> temporary = (it.assignedValue as RBooleanLiteral).isTrue
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
        breakpoint.isLogMessage = logMessage
        breakpoint.isLogStack = logStack
        breakpoint.isTemporary = temporary
      }
    }
  }

  companion object {
    const val DEFAULT_TIMEOUT = 20000
    const val maxInteropNumber = 8
    private val interopSpawner: InteropSpawner by lazy { InteropSpawner() }
    @Volatile
    private var state: RInterpreterStateImpl? = null

    class InteropSpawner {
      private val interopCache = ArrayList<RInterop>()
      private val interopPromises = ArrayList<Promise<RInterop>>()

      fun run(interpreter: RInterpreter, workingDirectory: String, tries: Int = 5): RInterop {
        val result : Promise<RInterop>
        synchronized(this) {
          while (interopPromises.size + interopCache.size < maxInteropNumber) {
            val promise = RInteropUtil.runRWrapperAndInterop(interpreter, workingDirectory)
            promise.onSuccess { interop ->
              synchronized(this@InteropSpawner) {
                if (interopPromises.contains(promise)) {
                  Disposer.register(ApplicationManager.getApplication(), interop)
                  interopCache.add(interop)
                  interopPromises.remove(promise)
                }
              }
            }
            interopPromises.add(promise)
          }
          if (interopCache.isNotEmpty()) {
            return interopCache.first().also {
              interopCache.remove(it)
            }
          }
          result = interopPromises.first().also {
            interopPromises.remove(it)
          }
        }
        try {
          val blockingGet = result.blockingGet(DEFAULT_TIMEOUT)
          return blockingGet!!
        }
        catch (e: Throwable) {
          if (tries > 0) {
            Thread.sleep(100)
            return run(interpreter, workingDirectory, tries - 1)
          }
        }
        throw IllegalStateException("Should not happen")
      }
    }

    private fun createRInterop(interpreter: RInterpreter): RInterop {
      return RInteropUtil.runRWrapperAndInterop(interpreter).blockingGet(DEFAULT_TIMEOUT)!!
    }

    private fun getRInterop(interpreter: RInterpreter): RInterop {
      val interop = interopSpawner.run(interpreter, interpreter.basePath)
      val currentState = state
      if (currentState == null) {
        interop.updateState().blockingGet(DEFAULT_TIMEOUT)
        state = interop.state as RInterpreterStateImpl
      } else {
        (interop.state as RInterpreterStateImpl).copyState(currentState)
      }
      return interop
    }
  }
}
