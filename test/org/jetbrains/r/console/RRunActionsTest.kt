/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.ActionManager
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.rinterop.RDebuggerTestHelper

class RRunActionsTest : RConsoleBaseTestCase() {
  fun testRunAction() {
    myFixture.configureByText("ff.R", """
      x <- 123
      y <- 456
      z = x * y
    """.trimIndent())
    val promise = AsyncPromise<Unit>()
    console.executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      override fun onCommandExecuted() {
        promise.setResult(Unit)
      }
    })
    ActionManager.getInstance().getAction("org.jetbrains.r.actions.RRunAction").actionPerformed(createAnActionEvent())
    promise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals((123 * 456).toString(), rInterop.executeCode("cat(z)").stdout)
  }

  fun testDebugAction() {
    loadFileWithBreakpointsFromText(name = "dd.R", text = """
      abc = 123
      abc = 456 # BREAKPOINT
      abc = 789
    """.trimIndent())
    val promise = AsyncPromise<Unit>()
    console.executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      override fun onCommandExecuted() {
        promise.setResult(Unit)
      }
    })
    val helper = RDebuggerTestHelper(rInterop)
    helper.invokeAndWait(true) {
      ActionManager.getInstance().getAction("org.jetbrains.r.actions.RDebugAction").actionPerformed(createAnActionEvent())
    }
    TestCase.assertEquals("123", rInterop.executeCode("cat(abc)").stdout)
    helper.invokeAndWait(true) {
      rInterop.debugCommandStepOver()
    }
    TestCase.assertEquals("456", rInterop.executeCode("cat(abc)").stdout)
    helper.invokeAndWait(false) {
      rInterop.debugCommandStepOver()
    }
    TestCase.assertEquals("789", rInterop.executeCode("cat(abc)").stdout)
  }
}
