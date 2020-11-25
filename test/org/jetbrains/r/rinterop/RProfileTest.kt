/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import java.io.File

class RProfileTest : RUsefulTestCase() {
  private lateinit var interpreter: RInterpreter

  override fun setUp() {
    super.setUp()
    setupMockInterpreterManager()
    interpreter = RInterpreterManager.getInterpreterBlocking(project, DEFAULT_TIMEOUT)!!
  }

  fun testRProfile() {
    doTest("""
      cat("Abacaba\n")
      x <- 111
      y <- 222
      4
    """.trimIndent()) { rInterop ->
      val output = StringBuilder()
      val promptPromise = AsyncPromise<Unit>()
      rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
        override fun onText(text: String, type: ProcessOutputType) {
          output.append(text)
        }

        override fun onPrompt(isDebug: Boolean) {
          promptPromise.setResult(Unit)
        }
      })
      rInterop.asyncEventsStartProcessing()
      promptPromise.blockingGet(DEFAULT_TIMEOUT)
      TestCase.assertEquals("111", rInterop.executeCode("cat(x)").stdout)
      TestCase.assertEquals("222", rInterop.executeCode("cat(y)").stdout)
      TestCase.assertTrue("Abacaba" in output.toString())
    }
  }

  private inline fun doTest(code: String, f: (RInterop) -> Unit) {
    val file = File(project.basePath, ".Rprofile")
    try {
      file.writeText(code)
      val rInterop = RInteropUtil.runRWrapperAndInterop(interpreter).blockingGet(DEFAULT_TIMEOUT)!!
      try {
        f(rInterop)
      } finally {
        Disposer.dispose(rInterop)
      }
    } finally {
      file.delete()
    }
  }

  companion object {
    private const val DEFAULT_TIMEOUT = 20000
  }
}