/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.r.psi.interpreter.RInterpreterManager
import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.junit.Ignore
import java.awt.Dimension
import java.io.File
import java.nio.file.Paths

@Ignore
class RBundledTestsTest : RProcessHandlerBaseTestCase() {
  private lateinit var tempDir: String
  override val customDeadline = 600L

  override fun setUp() {
    super.setUp()
    TestCase.assertTrue("RBundledTestsTest is currently not supported for remote interpreter", interpreter.isLocal())
    rInterop.asyncEventsStartProcessing()
    tempDir = FileUtil.createTempDirectory("tmpdir", null, true).absolutePath
    rInterop.setWorkingDir(tempDir)
  }

  // Tests from tools::testInstalledBasic
  //   Unused tests:
  //   reg-tests-2 - it uses R debugger
  //   utf8-regex - it uses readLines and input in source file
  //   reg-packages - it requires pdflatex to be installed
  fun testBasic_eval_etc() {
    // File 'eval-fns.R' is required for this test but it is missing in some R distribution
    if (rInterop.rVersion >= R_3_5 && execute("cat(file.exists(R.home('tests/eval-fns.R')))") == "FALSE") return
    doBasicTest("eval-etc", before = "assign('interactive', function(...) FALSE, envir = baseenv())")
  }

  fun testBasic_simple_true() = doBasicTest("simple-true")
  fun testBasic_arith_true() = doBasicTest("arith-true")
  fun testBasic_lm_tests() = doBasicTest("lm-tests")
  fun testBasic_ok_errors() = doBasicTest("ok-errors")
  fun testBasic_method_dispatch() = doBasicTest("method-dispatch")
  fun testBasic_array_subset() = doBasicTest("array-subset")
  fun testBasic_any_all() = doBasicTest("any-all")
  fun testBasic_d_p_q_r_tests() = doBasicTest("d-p-q-r-tests")
  fun testBasic_complex() = doBasicTest("complex", allowDiff = true)
  fun testBasic_print_tests() = doBasicTest("print-tests", allowDiff = true)
  fun testBasic_lapack() = doBasicTest("lapack", allowDiff = true)
  fun testBasic_datasets() = doBasicTest("datasets", allowDiff = true)
  fun testBasic_datetime() = doBasicTest("datetime", allowDiff = true)
  fun testBasic_iec60559() = doBasicTest("iec60559", allowDiff = true)
  fun testBasic_reg_tests_1a() = doBasicTest("reg-tests-1a", before = "Sys.setlocale('LC_ALL', 'C')")
  fun testBasic_reg_tests_1b() = doBasicTest("reg-tests-1b")
  fun testBasic_reg_tests_1c() = doBasicTest("reg-tests-1c", before = "Sys.setlocale('LC_ALL', 'C')")
  fun testBasic_reg_examples1() = doBasicTest("reg-examples1")
  fun testBasic_reg_examples2() = doBasicTest("reg-examples2")
  fun testBasic_p_qbeta_strict_tst() = doBasicTest("p-qbeta-strict-tst")
  fun testBasic_reg_IO() = doBasicTest("reg-IO")
  fun testBasic_reg_IO2() = doBasicTest("reg-IO2")
  fun testBasic_reg_plot() = doBasicTest("reg-plot")
  fun testBasic_reg_S4() = doBasicTest("reg-S4")
  fun testBasic_reg_BLAS() = doBasicTest("reg-BLAS")
  fun testBasic_reg_tests_3() = doBasicTest("reg-tests-3", allowDiff = true)
  fun testBasic_reg_examples3() = doBasicTest("reg-examples3", allowDiff = true)
  fun testBasic_reg_plot_latin1() = doBasicTest("reg-plot-latin1", allowDiff = true)
  fun testBasic_datetime2() = doBasicTest("datetime2", allowDiff = true)
  fun testBasic_isas_tests() = doBasicTest("isas-tests", allowDiff = true)
  fun testBasic_p_r_random_tests() = doBasicTest("p-r-random-tests", allowDiff = true)
  fun testBasic_demos() = doBasicTest("demos", before = "assign('interactive', function(...) FALSE, envir = baseenv())")
  fun testBasic_demos2() = doBasicTest("demos2")
  fun testBasic_primitives() = doBasicTest("primitives")
  fun testBasic_PCRE() = doBasicTest("PCRE", since = R_3_4)
  fun testBasic_CRANtools() = doBasicTest("CRANtools", since = R_3_4)
  fun testBasic_no_segfault() = doBasicTest("no-segfault")
  fun testBasic_internet() = doBasicTest("internet", allowDiff = true)
  fun testBasic_internet2() = doBasicTest("internet2", allowDiff = true)
  fun testBasic_libcurl() = doBasicTest("libcurl", allowDiff = true)

  private fun doBasicTest(name: String, allowDiff: Boolean = false, since: Version? = null, before: String? = null,
                          linuxOnly: Boolean = false) {
    if (since != null && rInterop.rVersion < since) return
    if (linuxOnly && !SystemInfo.isLinux) return

    execute("""
        Sys.setlocale("LC_COLLATE", "C")
        Sys.setenv(R_DEFAULT_PACKAGES = "")
        Sys.setenv(LC_COLLATE = "C")
        Sys.setenv(SRCDIR = ".")
        unlockBinding("interactive", baseenv())
        unlockBinding("quit", baseenv())
        assign("quit", function(...) stop("TEST_QUIT_CALLED"), envir = baseenv())
        unlockBinding("q", baseenv())
        assign("q", quit, envir = baseenv())
        options(keep.source = FALSE, error = NULL, warn = -1)
    """.trimIndent())
    File(execute("cat(R.home('tests'))")).listFiles()!!.filter { it.isFile || it.name == "Pkgs" }.forEach {
      it.copyRecursively(File(tempDir, it.name))
    }
    RGraphicsUtils.createGraphicsDevice(rInterop, Dimension(640, 480), null)

    val scriptName = "$name.R"
    val script = File(tempDir, scriptName)
    if (!script.exists()) {
      val generator = Paths.get(tempDir, scriptName + "in").toString()
      TestCase.assertTrue("${scriptName + "in"} does not exist", File(generator).exists())
      val cmd = GeneralCommandLine(RInterpreterManager.getInstance(project).interpreterLocation?.toLocalPathOrNull()!!,
                                   "--vanilla", "--slave", "-f", generator)
        .withWorkDirectory(File(tempDir))
      script.writeText(CapturingProcessHandler(cmd).runProcess().stdout)
    }

    before?.let { execute(it) }
    val output = executeFile(script.absolutePath)
    if (!allowDiff) {
      val expectedFile = File(tempDir, scriptName + "out.save")
      if (expectedFile.exists()) {
        val expected = expectedFile.readText()
        TestCase.assertEquals(prepareOutput(expected), prepareOutput(output))
      }
    }
  }


  fun testInstalledPackages() {
    val knownPackages = execute("""
        local({
          knownPackages <- tools:::.get_standard_package_names()
          cat(c(knownPackages $ base, knownPackages $ recommended), sep = "\n")
        })
    """.trimIndent()).lines().filter { it.isNotBlank() }
    knownPackages.forEach { pkg ->
      if (pkg == "tcltk" && SystemInfo.isMac) return@forEach
      val script = execute("""
        local({
          pkg <- "$pkg"
          if (pkg != "tcltk" || capabilities(pkg)) {
            pkgdir <- find.package(pkg, .Library)
            cat(tools:::.createExdotR(pkg, pkgdir, silent = TRUE, commentDonttest = TRUE))
          }
        })
      """.trimIndent())
        .let {
          if (it.isEmpty()) return@forEach
          File(tempDir, it)
        }
      if (!script.exists()) return@forEach
      withRInterop { newInterop ->
        println("Testing examples for package '$pkg'")
        execute("""
          Sys.setlocale("LC_COLLATE", "C")
          Sys.setenv(R_DEFAULT_PACKAGES = "")
          Sys.setenv(LC_COLLATE = "C")
          Sys.setenv(SRCDIR = ".")
          unlockBinding("quit", baseenv())
          assign("quit", function(...) stop("TEST_QUIT_CALLED"), envir = baseenv())
          unlockBinding("q", baseenv())
          assign("q", quit, envir = baseenv())
          options(keep.source = FALSE, error = NULL, warn = -1)
          Sys.unsetenv('DISPLAY')
          Sys.setlocale('LC_TIME', 'C')
          unlockBinding("interactive", baseenv())
          assign('interactive', function(...) FALSE, envir = baseenv())
        """.trimIndent(), newInterop)
        val output = executeFile(script.absolutePath, newInterop)
        val expectedFile = File(execute("cat(R.home('tests/Examples'))", newInterop), "$pkg-Ex.Rout.save")
        if (expectedFile.exists()) {
          println("  comparing output to ${expectedFile.name}")
          val expected = expectedFile.readText()
          val preparedExpected = prepareOutput(expected)
          val preparedOutput = prepareOutput(output)
          if (!StringUtil.equalsIgnoreWhitespaces(preparedExpected, preparedOutput)) {
            TestCase.assertEquals(preparedExpected, preparedOutput)
          }
        }
      }
    }
  }

  private fun execute(code: String, interop: RInteropImpl = rInterop): String {
    val result = interop.executeCode(code)
    TestCase.assertNull(result.exception)
    return result.stdout
  }

  private fun executeFile(file: String, interop: RInteropImpl = rInterop): String {
    val output = StringBuilder()
    val script = StringUtil.convertLineSeparators(File(file).readText())
    RConsoleExecuteActionHandler.splitCodeForExecution(project, prepareCode(script)).forEach { (code, _) ->
      code.lines().forEachIndexed { i, s ->
        output.append(if (i == 0) "> " else "+ ").append(s).append("\n")
      }
      val result = interop.executeCodeAsync(code, setLastValue = true) { s, _ ->
        output.append(s)
      }.blockingGet(Integer.MAX_VALUE)!!
      result.exception?.let {
        if ("TEST_QUIT_CALLED" in it) {
          TestCase.assertEquals("1", execute("cat(1)", interop))
          return@executeFile output.toString()
        }
        if (interop.executeCode("cat(is.null(getOption('error')))").stdout == "TRUE") {
          LOG.debug(output.toString())
          TestCase.fail("Error in $code:\n$it")
        }
      }
    }
    TestCase.assertEquals("1", execute("cat(1)", interop))
    return output.toString()
  }

  private inline fun withRInterop(f: (RInteropImpl) -> Unit) {
    val newInterop = RInteropUtil.runRWrapperAndInterop(interpreter).blockingGet(DEFAULT_TIMEOUT)!!
    try {
      newInterop.asyncEventsStartProcessing()
      newInterop.setWorkingDir(tempDir)
      f(newInterop)
      TestCase.assertTrue(newInterop.isAlive)
    } finally {
      Disposer.dispose(newInterop)
    }
  }

  companion object {
    private fun prepareOutput(output: String): String {
      return output.lineSequence()
        .dropWhile { !it.startsWith(">") }
        .map {
          it.trim()
            // <environment: 0x...>, <bytecode: 0x...>
            .replace(Regex(": 0x[0-9a-f]*>"), ">")
        }
        .let { lines ->
          val newLines = mutableListOf<String>()
          var drop = false
          lines.forEach {
            if (it == "> ## IGNORE_RDIFF_BEGIN") drop = true
            if (!drop) newLines.add(it)
            if (it == "> ## IGNORE_RDIFF_END") drop = false
          }
          newLines
        }
        .filter {
          it != ">" && it != "+" && it != "" &&
          "TEST_QUIT_CALLED" !in it &&
          !it.startsWith("Time elapsed:") &&
          // RWrapper creates some new objects in baseenv
          !it.startsWith("Number of all base objects:") &&
          !it.startsWith("Number of functions from these:") &&
          !it.startsWith("Number of base objects:") &&
          !it.startsWith("Number of functions in base:") &&
          // Warnings are not printed correctly in rwrapper
          !it.matches(Regex("There were \\d+ warnings \\(use warnings\\(\\) to see them\\)")) &&
          // This line is OS-dependent
          !it.contains("options(pager = \"console\")")
        }
        .let { lines ->
          val newLines = mutableListOf<String>()
          var drop = false
          lines.forEach {
            if (!drop || it.startsWith(">")) {
              // Warnings are not printed correctly in rwrapper
              if (it.startsWith("Warning message") ||
                  it.startsWith("> summary(warnings") ||
                  it.startsWith("In addition: Warning") ||
                  // System-dependent tests
                  (SystemInfo.isWindows && it.startsWith("> windowsFonts(")) ||
                  (SystemInfo.isWindows && it.startsWith("> if(.Platform${"$"}OS.type == \"unix\") {"))) {
                drop = true
              } else {
                drop = false
                newLines.add(it)
              }
            }
          }
          newLines
        }
        .joinToString("\n")
        // This test does not work on non-fresh R installation
        .replace("""
          > stopifnot(identical(
          +     sort(dependsOnPkgs("lattice", lib.loc = .Library)),
          +     c("Matrix", "mgcv", "nlme", "survival")))
        """.trimIndent(), "")
    }

    private fun prepareCode(s: String): String {
      var code = s
      if (SystemInfo.isWindows) {
        // Use other commands in windows
        code = code
          .replace("options(editor=\"touch\")", "options(editor = function(name, ...) name)")
          .replace("system(paste(\"cat\"", "system(paste(\"cmd /c type\"")
      }
      // This test does not work on non-fresh R installation
      code = code.replace("""
        stopifnot(identical(
            sort(dependsOnPkgs("lattice", lib.loc = .Library)),
            c("Matrix", "mgcv", "nlme", "survival")))
      """.trimIndent(), "")
      return code
    }
  }
}