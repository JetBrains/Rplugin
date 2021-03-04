/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class IdentifierCompletionTest : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testLocalBeforeGlobal() {
    doTest("""
      double_x <- 1232312
      dou<caret>
    """.trimIndent(), "double_x", "double")
  }

  fun testDontShowTwice() {
    doTest("""
      foo_baz <- 2312312
      foo_bar <- function(x)x+1
      foo_<caret>
    """.trimIndent(), "foo_bar", "foo_baz", strict = true)
  }

  fun testKeywords() {
    doTest("fun<caret>", "function")
    doTest("NA<caret>", "NA", "NA_character_", "NA_complex_", "NA_integer_", "NA_real_", "NaN")
    doTest("In<caret>", "Inf")
    doTest("TR<caret>", "TRUE")
    doTest("FA<caret>", "FALSE")
    doTest("br<caret>", "break")
    doApplyCompletionTest("fun<caret>", "function", "function (<caret>)")
    doApplyCompletionTest("ne<caret>", "next", "next<caret>")
  }

  fun testIn() {
    doTest("for (a in<caret>)", "in")
    doTest("for (i in<caret> a)", "in")
    doWrongVariantsTest("for (in<caret>)", "in")
    doWrongVariantsTest("in<caret>", "in")
    doWrongVariantsTest("for (i in a) in<caret>", "in")
    doWrongVariantsTest("for (i in a) { print 5; in<caret> }", "in")
    doWrongVariantsTest("for (i in (j in<caret> k))", "in")
  }

  fun testElse() {
    doTest("""
      if (T) 5
      el<caret>
    """.trimIndent(), "else")

    doTest("""
      if (T) { 5 }
      el<caret>
    """.trimIndent(), "else")

    doTest("""
      if (T) 5
      # Comment
      el<caret>
    """.trimIndent(), "else")

    doTest("""
      if (a %in% b) 5
      el<caret>
    """.trimIndent(), "else")

    doWrongVariantsTest("el<caret>", "else")

    doWrongVariantsTest("""
      if (T) 5
      else 6
      # Comment
      el<caret>
    """.trimIndent(), "else")

    doWrongVariantsTest("""
      if (T) 5
      print(5)
      el<caret>
    """.trimIndent(), "else")
  }

  fun testGlobal() {
    doTest("""
      foo_bar <- 321312321
      function(foo_baz) {
         foo_<caret>
      }
    """.trimIndent(), "foo_baz", "foo_bar", strict = true)
  }

  fun testClosure() {
    doTest("""
      foo_0 <- function(x)x + 1
      function(foo_1) {
        foo_2 <- 3212
        function(foo_3) {
          foo_<caret>
        }
      }
    """.trimIndent(), "foo_1", "foo_2", "foo_3", "foo_0", strict = true)
  }

  fun testFunctionArgument() {
    doTest("""
      widte <- 35435346
      widtk <- 321321
      png(widt<caret>)
    """.trimIndent(), "width", "widte", "widtk")
    doApplyCompletionTest("png(widt<caret>)", "width", "png(width = <caret>)")
  }

  fun testNoCompletionForParameters() {
    doTest("""
      foo <- 31312
      bar <- 312434
      function(<caret>)
    """.trimIndent(), strict = true)
  }

  fun testCompletionForPredefinedParameters() {
    doTest("""
      foo <- 31312
      far <- 312434
      function(x = f<caret>)
    """.trimIndent(), "far", "foo")
  }

  fun testCompletionForPredefinedParametersOrdering() {
    doTest("""
      maaa = 12312
      foo <- function(mapping = 123) { mapping + 1 } 
      foo(m<caret>)
    """.trimIndent(), "mapping", "maaa", "mad", "mahalanobis")
  }

  fun testCompletionForDeepPredefinedParameters() {

    doTest("""
      print(x, q<caret>)
    """.trimIndent(), "quote", withRuntimeInfo = true)

    doTest("""
      print(x, d<caret>)
    """.trimIndent(), "digits", withRuntimeInfo = true)

    doWrongVariantsTest("""
      print.factor(x, d<caret>)
    """.trimIndent(), "digits", withRuntimeInfo = true)

    val foosDeclaration = """
      foo <- function(x, ...) UseMethod("foo")
      foo.main <- function(x, parameter = 0) x + parameter
    """.trimIndent()

    rInterop.executeCode(foosDeclaration, true)
    doTest("""
      $foosDeclaration
      
      foo(x, p<caret>)
    """.trimIndent(), "parameter", withRuntimeInfo = true)
  }

  fun testCompletionForNotExportedDeepPredefinedParameters() {
    rInterop.executeCode("library(data.table)", true)
    doTest("""
      as.matrix(data.table(a = 1:3), rown<caret>)
    """.trimIndent(), "rownames", "rownames.value", withRuntimeInfo = true)
  }

  fun testCompletionDeepPredefinedParametersS3LikeS4() {
    doTest("new('Device', C<caret>)", "Class", withRuntimeInfo = true)
    doWrongVariantsTest("new('Device', ..<caret>)", "...", withRuntimeInfo = true)
    doWrongVariantsTest("new('Device', ava<caret>)", "available", withRuntimeInfo = true)
    doWrongVariantsTest("new('Device', li<caret>)", "lib.loc", withRuntimeInfo = true)
  }

  fun testNotImportedNamedArguments() {
    doTest("data.table::frollmean(table, al<caret>)", "algo", "align", withRuntimeInfo = true)
    doTest("frollmean(table, al<caret>)", "algo", "align", withRuntimeInfo = true)
  }

  fun testReadCsvExtraNamedArgs() {
    doTest("read.csv(nume<caret>)", "numerals")
    doTest("read.csv(fil<caret>)", "file", "fileEncoding")
  }

  fun testWriteCsvExtraNamedArgs() {
    doTest("write.csv(row.na<caret>)", "row.names")
    doTest("write.csv(fil<caret>)", "file", "fileEncoding")
  }

  fun testTryCatchExtraNamedArgs() {
    doTest("tryCatch(paste, colla<caret>)", "collapse")
    doTest("tryCatch(paste, err<caret>)", "error")
    doTest("tryCatch(paste, war<caret>)", "warning")
  }

  fun testNotLoadedLocalExtraNamedArgs() {
    doTest("""
      foo <- function(fileLocal, ...) {
        read.csv(...)
      }
      
      foo(fil<caret>)
    """.trimIndent(), "file", "fileEncoding", "fileLocal", withRuntimeInfo = true)
  }

  fun testLoadedLocalExtraNamedArgs() {
    rInterop.executeCode("""
      bar <- function(fileTwo, ...) {
        read.csv(...)
      }
      
      foo <- function(fileLocal, ...) {
        bar(...)
      }
    """.trimIndent(), true)

    doTest("foo(fil<caret>)", "file", "fileLocal", "fileTwo", withRuntimeInfo = true)
  }

  fun testLapplyFunctionExtraNamedArgs() {
    doTest("lapply(list, paste, colla<caret>)", "collapse")
  }

  fun testOverlappedExtraNamedArgs() {
    val fooDef = """
      foo <- function(fileLocal, ...) {
        read.csv(...)
      }
      
      bar <- function(fileLocal, ...) {
        read.csv(fileEncoding = fileLocal, ...)
      }
    """.trimIndent()

    doTest("""
      $fooDef
      foo(fil<caret>)
    """.trimIndent(), "file", "fileEncoding", "fileLocal", withRuntimeInfo = true)
    doTest("""
      $fooDef
      bar(fil<caret>)
    """.trimIndent(), "file", "fileLocal", withRuntimeInfo = true)
    doWrongVariantsTest("""
      $fooDef
      bar(fil<caret>)
    """.trimIndent(), "fileEncoding", withRuntimeInfo = true)
  }

  fun testOverlappedFunctionExtraNamedArgs() {
    val fooDef = """
      foo <- function(FUN_MY, ...) {
        lapply(list, ...)
      }
      
      bar <- function(FUN_MY, ...) {
        lapply(list, FUN = FUN_MY, ...)
      }
    """.trimIndent()

    doTest("""
      $fooDef
      foo(FU<caret>)
    """.trimIndent(), "FUN", "FUN_MY", withRuntimeInfo = true)
    doTest("""
      $fooDef
      bar(FU<caret>)
    """.trimIndent(), "FUN_MY", withRuntimeInfo = true)
    doWrongVariantsTest("""
      $fooDef
      bar(FU<caret>)
    """.trimIndent(), "FUN", withRuntimeInfo = true)
  }

  fun testLambdaInsideExtraNamedArgs() {
    val fooDef = """
      foo <- function(fileLocal, ...) {
        function(fileInside, ...) {
          read.table(...)
        }
      }
    """.trimIndent()

    doTest("""
      $fooDef
      foo(fil<caret>)
    """.trimIndent(), "fileLocal", withRuntimeInfo = true)
    doWrongVariantsTest("""
      $fooDef
      foo(fil<caret>)
    """.trimIndent(), "fileInside", "fileEncoding", withRuntimeInfo = true)

    val fooDefWithoutDots = """
      foo <- function(fileLocal, ...) {
        function(fileInside) {
          read.table(...)
        }
      }
    """.trimIndent()

    doTest("""
      $fooDefWithoutDots
      foo(fil<caret>)
    """.trimIndent(), "file", "fileEncoding", "fileLocal", withRuntimeInfo = true)
    doWrongVariantsTest("""
      $fooDefWithoutDots
      foo(fil<caret>)
    """.trimIndent(), "fileInside", withRuntimeInfo = true)
  }

  fun testLapplyLambdaFunctionExtraNamedArgs() {
    doTest("""
      lapply(list, function(xxxxx, xxxxxy, yyyyy, ...) {}, xxxx<caret>)
    """.trimIndent(), "xxxxx", "xxxxxy")
  }

  fun testQualifiedCallExtraNamedArgs() {
    doTest("""
      foo <- function(fileLocal, ...) {
        utils::read.csv(...)
      }
      
      foo(fil<caret>)
    """.trimIndent(), "file", "fileEncoding", "fileLocal", withRuntimeInfo = true)
  }

  fun testS3LapplyExtraNamedArgs() {
    doTest("""
      lapply(list, print, dig<caret>)
    """.trimIndent(), "digits", withRuntimeInfo = true)
  }

  fun testLapplyInsideExtraNamedArgs() {
    doTest("""
      foo <- function(digitData, digital, ...) {
        lapply(digitData, print.default, ...)
      }
      
      foo(list, digi<caret>)
    """.trimIndent(), "digital", "digitData", "digits", withRuntimeInfo = true)

    doWrongVariantsTest("""
      foo <- function(digitData, digital, ...) {
        lapply(digitData, print.default, ..., digits = 10)
      }
      
      foo(list, digi<caret>)
    """.trimIndent(), "digits", withRuntimeInfo = true)
  }

  fun testCallAsArgument() {
    doTest("""
      foo <- function(digit1, digit2, ...) {
        print.default(...) + digit1 + digit2
      }
      
      foo(digi<caret>)
    """.trimIndent(), "digit1", "digit2", "digits", withRuntimeInfo = true)
  }

  fun testCompletionForLocalVariableNames() {
    doTest("""
      foo_oo <- 31312
      foo_ar <- 312434
      foo_<caret> <- 3123124234
    """.trimIndent(), "foo_ar", "foo_oo", strict = true)
  }

  fun testCompletionFunction() {
    doApplyCompletionTest("""
      foo <- function(x, y, z) 0
      f<caret>
    """.trimIndent(), "foo", """
      foo <- function(x, y, z) 0
      foo(<caret>)
    """.trimIndent())
    doApplyCompletionTest("""
      foo <- function(x, y, z) 0
      f<caret>(1, 2, 3)
    """.trimIndent(), "foo", """
      foo <- function(x, y, z) 0
      foo(<caret>1, 2, 3)
    """.trimIndent())
    doApplyCompletionTest("""
      foo <- function() 0
      f<caret>
    """.trimIndent(), "foo", """
      foo <- function() 0
      foo()<caret>
    """.trimIndent())
    doApplyCompletionTest("""
      foo <- function() 0
      f<caret>()
    """.trimIndent(), "foo", """
      foo <- function() 0
      foo()<caret>
    """.trimIndent())
    doApplyCompletionTest("""
      foo <- function() 0
      f<caret>    ()
    """.trimIndent(), "foo", """
      foo <- function() 0
      foo    ()<caret>
    """.trimIndent())
  }

  fun testCompletionFunctionInRConsole() {
    doApplyCompletionTest("""
      p<caret>
    """.trimIndent(), "print", """
      print(<caret>)
    """.trimIndent(), true)
    doApplyCompletionTest("""
      ?p<caret>
    """.trimIndent(), "print", """
      ?print<caret>
    """.trimIndent(), true)
    doApplyCompletionTest("""
      ??p<caret>
    """.trimIndent(), "print", """
      ??print<caret>
    """.trimIndent(), true)
  }

  fun testCompletionInArgumentsWithoutComma() {
    fun doArgTest(text: String) {
      doTest("""
        my_var1 <- 1
        my_var2 <- 2
        $text
        """.trimIndent(), "my_var1", "my_var2", strict = true)
    }
    doArgTest("foo(10, my_<caret> 20)")
    doArgTest("foo(10, my_<caret> 20 30)")
    doArgTest("foo(10, my_<caret> 20, 30)")
    doArgTest("foo(10, my_<caret> param=20)")
    doArgTest("foo(10, my_<caret> \n  param=20)")
  }

  fun testNoCommaTailCompletion() {
    doTest("f(321<caret>)", strict = true)
    doTest("f(\"321\"<caret>)", strict = true)
  }

  fun testOperatorCompletion() {
    fun doOperatorTest(text: String, textAfterCompletion: String) {
      val header = """
         `%xxyyzz_operator%` <- function(x, y) x + y + x + y
         xxyyzz_not_operator <- function(x, y) x + y - x + y
         `%xxyyzz_one_more_operator%` <- function(x, y) 42
      """.trimIndent()
      val fileText = """
        $header
        $text
        """.trimIndent()
      doTest(fileText, "%xxyyzz_one_more_operator%", "%xxyyzz_operator%", strict = true)

      doApplyCompletionTest(fileText, "%xxyyzz_operator%", """
        $header
        $textAfterCompletion
        """.trimIndent())
    }

    doOperatorTest("1 %xxyyzz_<caret>", "1 %xxyyzz_operator%<caret>")
    doOperatorTest("%xxyyzz_<caret>", "%xxyyzz_operator%<caret>")
    doOperatorTest("1 %xxyyzz_<caret>%", "1 %xxyyzz_operator%<caret>")
    doOperatorTest("%xxyyzz_<caret>%", "%xxyyzz_operator%<caret>")
  }

  fun testLookupPackageName() {
    myFixture.configureByText("foo.R", """gau<caret>""")
    val result = myFixture.completeBasic()!!.first { it.lookupString == "gaussian" }
    val presentation = LookupElementPresentation()
    result.renderElement(presentation)
    TestCase.assertEquals("stats", presentation.typeText)
  }

  fun testInLoadedPackage() {
    fun doTest(expectedPackage: String) {
      myFixture.configureByText("foo.R", """fil<caret>""")
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
      val result = myFixture.completeBasic()!!.first { it.lookupString == "filter" }
      val presentation = LookupElementPresentation()
      result.renderElement(presentation)
      TestCase.assertEquals(expectedPackage, presentation.typeText)
    }

    doTest("stats")
    rInterop.executeCode("library(dplyr)")
    rInterop.invalidateCaches()
    doTest("dplyr")
    rInterop.executeCode("detach('package:dplyr')")
    rInterop.invalidateCaches()
    doTest("stats")
  }

  private fun doWrongVariantsTest(text: String, vararg variants: String, withRuntimeInfo: Boolean = false) {
    myFixture.configureByText("foo.R", text)
    if (withRuntimeInfo) {
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    val result = myFixture.completeBasic()
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    UsefulTestCase.assertDoesntContain(lookupStrings, *variants)
  }

  private fun doTest(text: String, vararg variants: String, strict: Boolean = false, withRuntimeInfo: Boolean = false) {
    myFixture.configureByText("foo.R", text)
    if (withRuntimeInfo) {
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    val result = myFixture.completeBasic()
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    if (strict) {
      assertOrderedEquals(lookupStrings, *variants)
    }
    else {
      assertContainsOrdered(lookupStrings, *variants)
    }
  }
}
