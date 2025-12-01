/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.blockingGetAndDispatchEvents

class RConsoleDebuggerTestHelper(console: RConsoleViewImpl) {
  private var promise = AsyncPromise<Boolean>()

  init {
    console.executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      override fun onCommandExecuted() {
        promise.setResult(console.executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT)
      }
    })
  }

  fun <R> invokeAndWait(expectedDebug: Boolean, f: () -> R): R {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    val result = f()
    TestCase.assertEquals(expectedDebug, promise.blockingGetAndDispatchEvents(
      DEFAULT_TIMEOUT))
    promise = AsyncPromise()
    return result
  }

  companion object {
    private const val DEFAULT_TIMEOUT = 20000
  }
}
