/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.rinterop.RInteropAsyncEventsListener

class RConsoleReadlineTest : RConsoleBaseTestCase() {
  fun testReadline() {
    val promise1 = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInteropAsyncEventsListener {
      override fun onRequestReadLn(prompt: String) {
        promise1.setResult(Unit)
      }
    })
    val promise2 = console.executeText("s = paste0('[', readline(), ']')\n")
    promise1.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    var remaining = 20
    while (console.executeActionHandler.state != RConsoleExecuteActionHandler.State.READ_LN) {
      remaining--
      TestCase.assertTrue(remaining > 0)
      Thread.sleep(15)
    }
    console.executeText("line\n")
    promise2.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[line]", rInterop.executeCode("cat(s)").stdout)
  }
}