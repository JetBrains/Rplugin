/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.text.StringUtil
import com.sun.jna.Platform
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
    TestCase.assertTrue("abcd654" in RRef.expressionRef("a", rInterop).evaluateAsText())
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

      override fun onPrompt(isDebug: Boolean) {
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

  private fun makeRCommand(code: String): String {
    return "${RInterpreterUtil.suggestHomePath()} --no-save --no-restore --slave -e \"${StringUtil.escapeStringCharacters(code)}\""
  }

  private fun makeRInteractiveCommand(): String {
    return "${RInterpreterUtil.suggestHomePath()} --no-save --no-restore --slave --interactive"
  }

  companion object {
    private val ECHO = if (Platform.isWindows()) "cmd /c echo" else "echo"
  }
}
