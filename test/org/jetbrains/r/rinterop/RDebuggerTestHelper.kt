/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.blockingGetAndDispatchEvents

class RDebuggerTestHelper(rInterop: RInterop) {
  private var promise = AsyncPromise<Boolean>()

  init {
    rInterop.asyncEventsStartProcessing()
    val textPromise = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onPrompt(isDebug: Boolean, isDebugStep: Boolean, isBreakpoint: Boolean) {
        promise.setResult(isDebug)
      }

      override fun onText(text: String, type: ProcessOutputType) {
        if (READY_STR in text) {
          promise = AsyncPromise<Boolean>()
          textPromise.setResult(Unit)
        }
      }
    })
    invokeAndWait(false) {
      rInterop.replExecute("cat('$READY_STR\\n')")
      textPromise.blockingGet(DEFAULT_TIMEOUT)
    }
  }

  fun <R> invokeAndWait(expectedDebug: Boolean, f: () -> R): R {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    val result = f()
    TestCase.assertEquals(expectedDebug, promise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT))
    promise = AsyncPromise()
    return result
  }

  companion object {
    private const val DEFAULT_TIMEOUT = 3000
    private const val READY_STR = "__READY__"
  }
}
