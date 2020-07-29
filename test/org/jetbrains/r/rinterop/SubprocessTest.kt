/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class SubprocessTest : RProcessHandlerBaseTestCase() {
  fun testEcho() {
    val result = rInterop.executeCodeAsync("system('$ECHO abcd123')", withEcho = false).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals("abcd123${System.lineSeparator()}", result.stdout)
    TestCase.assertEquals("", result.stderr)
    TestCase.assertEquals(null, result.exception)
  }

  fun testEchoIntern() {
    val result = rInterop.executeCodeAsync("a <- system('$ECHO abcd123', intern = TRUE)").blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals("", result.stdout)
    TestCase.assertEquals("", result.stderr)
    TestCase.assertEquals(null, result.exception)
    TestCase.assertEquals("abcd123", rInterop.executeCode("cat(a)").stdout)
  }

  fun testStdoutStderr() {
    val result = rInterop.executeCodeAsync("""
      system("${StringUtil.escapeStringCharacters(makeRCommand("cat('abcd321'); cat('xxyyzz', file = stderr())"))}")
    """.trimIndent(), withEcho = false).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertTrue("result = $result", "abcd321" in result.stdout)
    TestCase.assertTrue("result = $result", "xxyyzz" in result.stderr)
    TestCase.assertEquals(null, result.exception)
  }

  fun testStdoutStderrIntern() {
    val result = rInterop.executeCodeAsync("""
      a <- system("${StringUtil.escapeStringCharacters(makeRCommand("cat('abcd654'); cat('zzyyxx', file=stderr())"))}", intern = TRUE)
    """.trimIndent()).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals("", result.stdout)
    TestCase.assertTrue("result = $result", "zzyyxx" in result.stderr)
    TestCase.assertEquals(null, result.exception)
    TestCase.assertTrue("abcd654" in RReference.expressionRef("a", rInterop).evaluateAsTextAsync().get())
  }

  fun testInput() {
    val promptPromise = AsyncPromise<Unit>()
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      var done = false

      override fun onText(text: String, type: ProcessOutputType) {
        when (type) {
          ProcessOutputType.STDOUT -> stdout.append(text)
          ProcessOutputType.STDERR -> stderr.append(text)
        }
      }

      override fun onSubprocessInput() {
        rInterop.replSendReadLn("1002 + 999${System.lineSeparator()}")
        rInterop.replSendReadLn("cat('hi', file = stderr())${System.lineSeparator()}")
        rInterop.replSendEof()
        done = true
      }

      override fun onPrompt(isDebug: Boolean, isDebugStep: Boolean, isBreakpoint: Boolean) {
        if (done) {
          promptPromise.setResult(Unit)
        }
      }
    })
    rInterop.asyncEventsStartProcessing()
    rInterop.replExecute("""
      system("${StringUtil.escapeStringCharacters(makeRInteractiveCommand())}")
    """.trimIndent())
    promptPromise.blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertTrue("stdout = $stdout", "[1] 2001" in stdout.toString())
    TestCase.assertTrue("stderr = $stderr", "hi" in stderr.toString())
  }

  fun testBackground() {
    val stdoutBuf = StringBuilder()
    val promise = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      var done = false

      override fun onText(text: String, type: ProcessOutputType) {
        if (type == ProcessOutputType.STDOUT) stdoutBuf.append(text)
        if ("[1] 222" in stdoutBuf) promise.setResult(Unit)
      }
    })
    rInterop.asyncEventsStartProcessing()
    rInterop.replExecute("""
      system("${StringUtil.escapeStringCharacters(makeRCommand("Sys.sleep(1); print(200 + 20 + 2)"))}", wait = FALSE)
      print(100 + 10 + 1)
    """.trimIndent())
    promise.blockingGet(DEFAULT_TIMEOUT)
    val stdout = stdoutBuf.toString()
    val pos1 = stdout.indexOf("[1] 111")
    val pos2 = stdout.indexOf("[1] 222")
    TestCase.assertTrue(pos1 != -1)
    TestCase.assertTrue(pos2 != -1)
    TestCase.assertTrue(pos1 < pos2)
  }

  private fun makeRCommand(code: String): String {
    return "${RInterpreterUtil.suggestHomePath()} --no-save --no-restore --slave -e \"${StringUtil.escapeStringCharacters(code)}\""
  }

  private fun makeRInteractiveCommand(): String {
    return "${RInterpreterUtil.suggestHomePath()} --no-save --no-restore --slave --interactive"
  }

  companion object {
    private val ECHO = if (SystemInfo.isWindows) "cmd /c echo" else "echo"
  }
}
