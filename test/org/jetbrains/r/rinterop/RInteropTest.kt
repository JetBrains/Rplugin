/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RInteropTest : RProcessHandlerBaseTestCase() {
  fun testExecuteCode() {
    val result = rInterop.executeCode("""
      print(123)
      x = 1
      x + 2
      cat("err\n", file=stderr())
    """.trimIndent())
    TestCase.assertEquals("[1] 123\n[1] 3\n", result.stdout)
    TestCase.assertEquals("err\n", result.stderr)
  }

  fun testExecuteBigCommand() {
    rInterop.executeCode("x <- 123")
    val result = rInterop.executeCode("""print(if (typeof(environment()${'$'}x) == "closure" && isdebugged(environment()${'$'}x)) attr(environment()${'$'}x, "original") else environment()${'$'}x)""")
    TestCase.assertEquals("[1] 123\n", result.stdout)
    TestCase.assertEquals("", result.stderr)
  }

  fun testExecuteLongStringLiteral() {
    val long = "a".repeat(1000)
    val result = rInterop.executeCode("cat(\"$long\")")
    TestCase.assertEquals(long, result.stdout)
    TestCase.assertEquals("", result.stderr)
  }

  fun testExecuteMultilineWithFor() {
    val result = rInterop.executeCode("""
      x <- 312312
      print(x)
      for (i in c(1,2,3,4,5,6)) {
        print(i)
      }
      z <- function(x) {
        print(x)
      }
      z(123)
      for (i in c(1,2,3,4,5,6,7,8,9)) {
        z(i)
      }
      print("Finish")
    """.trimIndent())
    TestCase.assertEquals("""
      [1] 312312
      [1] 1
      [1] 2
      [1] 3
      [1] 4
      [1] 5
      [1] 6
      [1] 123
      [1] 1
      [1] 2
      [1] 3
      [1] 4
      [1] 5
      [1] 6
      [1] 7
      [1] 8
      [1] 9
      [1] "Finish"
    """.trimIndent(), result.stdout.trim())
    TestCase.assertEquals("", result.stderr)
  }

  fun testExecuteError() {
    rInterop.executeCode("""
      Sys.setlocale(\"LC_MESSAGES\", 'en_GB.UTF-8')
      Sys.setenv(LANG = \"en_US.UTF-8\")
    """.trimIndent())
    val result = rInterop.executeCode("foo()")
    TestCase.assertEquals(result.stdout, "")
    TestCase.assertTrue(result.stderr.contains("could not find function \"foo\""))
  }

  fun testExecuteCodeInterrupt() {
    val promise = rInterop.executeCodeAsync("x <- 0; while (TRUE) x = x + 1")
    Thread.sleep(100)
    promise.cancel()
    TestCase.assertEquals("[1] 123\n", rInterop.executeCodeAsync("123").blockingGet(DEFAULT_TIMEOUT)?.stdout)
  }

  fun testReplReadline() {
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onRequestReadLn(prompt: String) {
        rInterop.replSendReadLn("100")
        rInterop.replSendReadLn("200")
        rInterop.replSendReadLn("300")
        rInterop.removeAsyncEventsListener(this)
      }
    })
    rInterop.asyncEventsStartProcessing()
    rInterop.executeCode("x <- 0")
    rInterop.replExecute("""
      for (i in 1:3) {
        x = x + as.integer(readline())
      }
    """.trimIndent()).blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] 600\n", rInterop.executeCode("x").stdout)
  }

  fun testViewRequest() {
    val promise = AsyncPromise<Pair<RValue, String>>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onViewRequest(ref: RReference, title: String, value: RValue): Promise<Unit> {
        promise.setResult(value to title)
        return resolvedPromise()
      }
    })
    rInterop.asyncEventsStartProcessing()
    rInterop.replExecute("tt = data.frame(x = 1:9, y = 2:10)")
    rInterop.replExecute("View(tt, 'abcdd')")
    val (value, title) = promise.blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals("abcdd", title)
    TestCase.assertTrue(value is RValueDataFrame)
    TestCase.assertEquals(9, (value as RValueDataFrame).rows)
  }

  fun testParallelPrint() {
    if (!SystemInfo.isUnix) {
      // As stated in parallel::mclapply documentation, it does not work on Windows
      return
    }
    rInterop.asyncEventsStartProcessing()
    val stdoutPromise = AsyncPromise<String>()
    val stderrPromise = AsyncPromise<String>()
    val expectedStdoutLength = 9
    val expectedStderrLength = 9 * 10 / 2
    val listener = object : RInterop.AsyncEventsListener {
      val stdout = StringBuilder()
      val stderr = StringBuilder()

      override fun onText(text: String, type: ProcessOutputType) {
        when (type) {
          ProcessOutputType.STDOUT -> {
            stdout.append(text)
            if (stdout.length >= expectedStdoutLength) stdoutPromise.setResult(stdout.toString())
          }
          ProcessOutputType.STDERR -> {
            stderr.append(text)
            if (stderr.length >= expectedStderrLength) stderrPromise.setResult(stderr.toString())
          }
        }
      }
    }
    rInterop.addAsyncEventsListener(listener)

    rInterop.replExecute("""
      a <- parallel::mclapply(1:9, mc.cores = 2, function(x) {
        cat(x)
        for (i in 1:x) cat("!", file=stderr())
        return(x*x)
      })
    """.trimIndent())
    TestCase.assertEquals("123456789", stdoutPromise.blockingGet(DEFAULT_TIMEOUT)!!.asSequence().sorted().joinToString(""))
    TestCase.assertEquals("!".repeat(expectedStderrLength), stderrPromise.blockingGet(DEFAULT_TIMEOUT))
    TestCase.assertEquals((1..9).map { (it * it).toString() }, RReference.expressionRef("as.character(a)", rInterop).getDistinctStrings())
    rInterop.removeAsyncEventsListener(listener)
  }

  fun testDataTablePrint() {
    rInterop.asyncEventsStartProcessing()

    fun doTest(command: String, expectOutput: Boolean) {
      var wasOutput = false
      val promise = AsyncPromise<Unit>()
      val listener = object : RInterop.AsyncEventsListener {
        override fun onText(text: String, type: ProcessOutputType) {
          if (type == ProcessOutputType.STDOUT && text.isNotBlank()) {
            wasOutput = true
          }
        }
        override fun onPrompt(isDebug: Boolean) {
          promise.setResult(Unit)
        }
      }
      rInterop.addAsyncEventsListener(listener)
      rInterop.replExecute(command)
      promise.blockingGet(DEFAULT_TIMEOUT)
      TestCase.assertEquals(expectOutput, wasOutput)
      rInterop.removeAsyncEventsListener(listener)
    }

    rInterop.executeCode("library(data.table)")
    rInterop.executeCode("tt <- data.table(a = 1:5)")
    doTest("tt", true)
    doTest("print(tt)", true)
    doTest("tt[, b := a * 2]", false)
    doTest("print(tt[, c := a * 2])", true)
  }

  fun testWarning() {
    TestCase.assertTrue("Warning: msg1" in rInterop.executeCode("warning('msg1')").stderr)
    TestCase.assertTrue("Warning in foobar() : msg2" in rInterop.executeCode("""
      foobar <- function() {
        warning("msg2")
      }
      foobar()
    """.trimIndent()).stderr)
  }

  fun testSetValue() {
    rInterop.executeCode("aa <- 10; bb <- 20; cc <- 30")
    RReference(ProtoUtil.envMemberRefProto(rInterop.globalEnvRef.proto, "bb"), rInterop)
      .setValue(RReference.expressionRef("123", rInterop))
      .blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] 123", rInterop.executeCode("bb").stdout.trim())

    rInterop.executeCode("s1 <- list(1,2,3,4); s2 <- s1")
    RReference(ProtoUtil.listElementRefProto(
      ProtoUtil.envMemberRefProto(rInterop.globalEnvRef.proto, "s2"), 1), rInterop)
      .setValue(RReference.expressionRef("'Hello'", rInterop))
      .blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] 2", rInterop.executeCode("s1[[2]]").stdout.trim())
    TestCase.assertEquals("[1] \"Hello\"", rInterop.executeCode("s2[[2]]").stdout.trim())

    rInterop.executeCode("s1 <- c(1,2,3,4); s2 <- s1")
    RReference(ProtoUtil.listElementRefProto(
      ProtoUtil.envMemberRefProto(rInterop.globalEnvRef.proto, "s2"), 1), rInterop)
      .setValue(RReference.expressionRef("321", rInterop))
      .blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] 2", rInterop.executeCode("s1[2]").stdout.trim())
    TestCase.assertEquals("[1] 321", rInterop.executeCode("s2[2]").stdout.trim())
  }

  fun testLastValue() {
    rInterop.executeCodeAsync("10 * 20 * 30", setLastValue = true, withEcho = true).blockingGet(DEFAULT_TIMEOUT)
    rInterop.executeCodeAsync("'no'", setLastValue = false, withEcho = true).blockingGet(DEFAULT_TIMEOUT)
    val result = rInterop.executeCode("cat(.Last.value)")
    TestCase.assertEquals("6000", result.stdout)
  }

  fun testLoadedPackages() {
    var packages = rInterop.loadedPackages.getWithCheckCancel()
    TestCase.assertFalse("compiler" in packages)
    TestCase.assertFalse("tools" in packages)

    rInterop.executeCode("library(compiler); library(tools)")
    rInterop.invalidateCaches()
    packages = rInterop.loadedPackages.getWithCheckCancel()
    TestCase.assertTrue("compiler" in packages)
    TestCase.assertTrue("tools" in packages)

    rInterop.executeCode("detach('package:compiler'); detach('package:tools')")
    rInterop.invalidateCaches()
    packages = rInterop.loadedPackages.getWithCheckCancel()
    TestCase.assertFalse("compiler" in packages)
    TestCase.assertFalse("tools" in packages)
  }

  fun testToplevelHandlers() {
    rInterop.replExecute("""
      addTaskCallback(function(...) {
        result <<- "Toplevel handler was called!"
      })
    """.trimIndent()).blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("Toplevel handler was called!", rInterop.executeCode("cat(result)").stdout)
  }

  fun testStackOverflow() {
    rInterop.executeCode("f <- function() f()")
    var promptPromise = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onPrompt(isDebug: Boolean) {
        promptPromise.setResult(Unit)
      }
    })
    rInterop.asyncEventsStartProcessing()
    promptPromise.blockingGet(DEFAULT_TIMEOUT)
    promptPromise = AsyncPromise<Unit>()
    rInterop.replExecute("f()").blockingGet(DEFAULT_TIMEOUT)
    promptPromise.blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("123", rInterop.executeCode("cat(123)").stdout)
  }

  fun testStackOverflowDebug() {
    rInterop.executeCode("f <- function() f()")
    var promptPromise = AsyncPromise<Unit>()
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onPrompt(isDebug: Boolean) {
        promptPromise.setResult(Unit)
      }
    })
    rInterop.asyncEventsStartProcessing()
    promptPromise.blockingGet(DEFAULT_TIMEOUT)
    promptPromise = AsyncPromise<Unit>()
    rInterop.replExecute("f()", isDebug = true).blockingGet(DEFAULT_TIMEOUT)
    promptPromise.blockingGet(DEFAULT_TIMEOUT)
    TestCase.assertEquals("123", rInterop.executeCode("cat(123)").stdout)
    TestCase.assertFalse(rInterop.isDebug)
  }

  fun testSaveLoadGlobalEnv() {
    rInterop.executeCode("x <- 1; y <- 2")
    val tempFile = FileUtil.createTempFile("tmp-", ".RData", true)
    rInterop.saveGlobalEnvironment(tempFile.absolutePath).blockingGet(DEFAULT_TIMEOUT)
    assertTrue(tempFile.length() > 0)
    rInterop.executeCode("rm(list = ls())")
    rInterop.loadEnvironment(tempFile.absolutePath, "foo").blockingGet(DEFAULT_TIMEOUT)
    val (stdoutLocal, _, _) = rInterop.executeCode("cat(foo${'$'}x + foo${'$'}y)")
    assertEquals("3", stdoutLocal)
    rInterop.loadEnvironment(tempFile.absolutePath, "")
    val (stdoutGlobal, _, _) = rInterop.executeCode("cat(x + y)")
    assertEquals("3", stdoutGlobal)
  }

  fun testUserAgent() {
    val stdout = rInterop.executeCode("cat(getOption('HTTPUserAgent'))").stdout
    assertTrue("Actual results: $stdout", stdout.startsWith("Rkernel"))
  }
}