/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import java.io.File

class SubprocessTest : RProcessHandlerBaseTestCase() {
  private val echoCommand get() = if (interpreter.hostOS == OperatingSystem.WINDOWS) "cmd /c echo" else "echo"

  fun testEcho() {
    val result = rInterop.executeCodeAsync("system('$echoCommand abcd123')", withEcho = false).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals("abcd123${System.lineSeparator()}", result.stdout)
    TestCase.assertEquals(null, result.exception)
  }

  fun testEchoIntern() {
    val result = rInterop.executeCodeAsync("a <- system('$echoCommand abcd123', intern = TRUE)").blockingGet(DEFAULT_TIMEOUT)!!
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

  fun testInputFile() {
    val inFile = FileUtil.createTempFile("a", ".txt", true)
    inFile.writeText("paste(rep('z', 10), collapse='')")
    rInterop.replExecute("""
      s <- system2("${StringUtil.escapeStringCharacters(interpreter.interpreterPathOnHost)}",
                   c("--vanilla", "--slave", "--interactive"),
                   stdin = "${StringUtil.escapeStringCharacters(inFile.absolutePath)}", stdout = TRUE)
    """.trimIndent()).blockingGet(DEFAULT_TIMEOUT)
    val out = rInterop.executeCode("cat(s)").stdout
    TestCase.assertTrue(out, "[1] \"zzzzzzzzzz\"" in out)
  }

  fun testInputText() {
    val inFile = FileUtil.createTempFile("a", ".txt", true)
    inFile.writeText("paste(rep('z', 10), collapse='')")
    rInterop.replExecute("""
      s <- system2("${StringUtil.escapeStringCharacters(interpreter.interpreterPathOnHost)}",
                   c("--vanilla", "--slave", "--interactive"),
                   input = "paste(rep('z', 10), collapse='')", stdout = TRUE)
    """.trimIndent()).blockingGet(DEFAULT_TIMEOUT)
    val out = rInterop.executeCode("cat(s)").stdout
    TestCase.assertTrue(out, "[1] \"zzzzzzzzzz\"" in out)
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

  private enum class OutputVariants {
    IGNORE, COLLECT, CONSOLE, FILE
  }

  fun testSystem2() {
    val dir = FileUtil.createTempDirectory("r_system2_test", null, true)
    val script = File(dir, "a.R")
    script.writeText("""
      cat("OUT_1\n")
      cat("ERR_2\n", file=stderr())
    """.trimIndent())
    val outFile = File(dir, "out.txt")
    val errFile = File(dir, "err.txt")
    for (outVariant in OutputVariants.values()) {
      for (errVariant in OutputVariants.values()) {
        if (errVariant == OutputVariants.COLLECT && outVariant != OutputVariants.COLLECT) {
          continue // Not allowed by R
        }
        val outValue = when (outVariant) {
          OutputVariants.IGNORE -> "FALSE"
          OutputVariants.COLLECT -> "TRUE"
          OutputVariants.CONSOLE -> "''"
          OutputVariants.FILE -> "\"${StringUtil.escapeStringCharacters(outFile.absolutePath)}\""
        }
        val errValue = when (errVariant) {
          OutputVariants.IGNORE -> "FALSE"
          OutputVariants.COLLECT -> "TRUE"
          OutputVariants.CONSOLE -> "''"
          OutputVariants.FILE -> "\"${StringUtil.escapeStringCharacters(errFile.absolutePath)}\""
        }
        val output = rInterop.executeCodeAsync(
          """s <- system2("${StringUtil.escapeStringCharacters(interpreter.interpreterPathOnHost)}",
            |              c("--vanilla", "--slave", "-f",
            |                "${StringUtil.escapeStringCharacters(script.absolutePath)}"),
            |              stdout = $outValue, stderr = $errValue)""".trimMargin(),
          isRepl = true, returnOutput = true).blockingGet(DEFAULT_TIMEOUT)!!
        val s = rInterop.executeCode("cat(s)").stdout

        TestCase.assertEquals(outVariant == OutputVariants.COLLECT, "OUT_1" in s)
        TestCase.assertEquals(outVariant == OutputVariants.CONSOLE, "OUT_1" in output.stdout)
        TestCase.assertEquals(outVariant == OutputVariants.FILE,
                              outFile.let { it.exists() && it.readText().trim() == "OUT_1" })
        TestCase.assertEquals(errVariant == OutputVariants.COLLECT, "ERR_2" in s)
        TestCase.assertEquals(errVariant == OutputVariants.CONSOLE, "ERR_2" in output.stderr)
        TestCase.assertEquals(errVariant == OutputVariants.FILE,
                              errFile.let { it.exists() && it.readText().trim() == "ERR_2" })

        outFile.takeIf { it.exists() }?.delete()
        errFile.takeIf { it.exists() }?.delete()
      }
    }
  }

  private fun makeRCommand(code: String): String {
    return "${interpreter.interpreterPathOnHost} --vanilla --slave -e \"${StringUtil.escapeStringCharacters(code)}\""
  }

  private fun makeRInteractiveCommand(): String {
    return "${interpreter.interpreterPathOnHost} --vanilla --slave --interactive"
  }
}
