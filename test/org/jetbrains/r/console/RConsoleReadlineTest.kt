/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.rinterop.RInterop

class RConsoleReadlineTest : RConsoleBaseTestCase() {
  fun testReadline() {
    val promise1 = AsyncPromise<Unit>()
    val promise2 = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onRequestReadLn(prompt: String) {
        promise1.setResult(Unit)
      }

      override fun onPrompt(isDebug: Boolean) {
        promise2.setResult(Unit)
      }
    })
    console.executeText("s = paste0('[', readline(), ']')\n")
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    promise1.blockingGet(DEFAULT_TIMEOUT)
    console.executeText("line\n")
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    promise2.blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[line]", rInterop.executeCode("cat(s)").stdout)
  }
}