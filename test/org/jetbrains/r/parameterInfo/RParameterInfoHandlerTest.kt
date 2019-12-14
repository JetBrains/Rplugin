/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.parameterInfo

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.EditorHintFixture
import com.intellij.util.ui.UIUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RParameterInfoHandlerTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testEmptyList() {
    doTest("print(<caret>)", "<x>, ...")
  }

  fun testLocalFunction() {
    doTest("""
      foo <- function(fst, snd, thd) { 42 }
      
      foo(40, 4<caret>1, 43)
    """.trimIndent(), "fst, <snd>, thd")
  }

  fun testDefaultValues() {
    doTest("""
      foo <- function(fst = 42, snd, thd = f(call(42, "aba"), "daba"), fth = to_long_call(to_long_call(to_long_call(50)))) { 42 }
      
      foo(40, 41, 4<caret>3, 44)
    """.trimIndent(), "fst = 42, snd, <thd = f(call(42, \"aba\"), \"daba\")>, fth = ...")

    doTest("""
      foo <- function(aa = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', bb = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") { 42 }
      
      foo(<caret>, "ba")
    """.trimIndent(), "<aa = '...'>, bb = \"...\"")
  }

  fun testArgsPermutation() {
    doTest("""
      foo <- function(fst, snd, thd = 42, fth = foo(1, 2, 3, 4)) { 42 }
      
      foo(40, thd = 4<caret>1, snd = 43, 42)
    """.trimIndent(), "fst, <[thd = 42]>, [snd], fth = foo(1, 2, 3, 4)")

    doTest("""
      foo <- function(fst, snd) { 42 }
      
      foo(40, fst = 4<caret>1)
    """.trimIndent(), "[snd], <[fst]>")
  }

  fun testCaretAroundComma() {
    val fooDeclaration = """
      foo <- function(fst, snd) { 42 }
    """.trimIndent()
    doTest("""
      $fooDeclaration
      foo(41<caret>, 43)
    """.trimIndent(), "<fst>, snd")

    doTest("""
      $fooDeclaration
      foo(41,<caret> 43)
    """.trimIndent(), "fst, <snd>")

    doTest("""
      $fooDeclaration
      foo(41  $t $n $n   $t<caret>  $t , $t  $n  $t    43)
    """.trimIndent(), "<fst>, snd")

    doTest("""
      $fooDeclaration
      foo(41 $t   $n $n $t  ,  $t $n $n $t<caret>  $n $n $n $t  43)
    """.trimIndent(), "fst, <snd>")
  }

  fun testCommaAroundBoundaries() {
    val fooDeclaration = """
      foo <- function(fst, snd) { 42 }
    """.trimIndent()
    doTest("""
      $fooDeclaration
      foo(<caret>41, 43)
    """.trimIndent(), "<fst>, snd")

    doTest("""
      $fooDeclaration
      foo(41,43<caret>)
    """.trimIndent(), "fst, <snd>")

    doTest("""
      $fooDeclaration
      foo($n   $n $t   $t<caret>$t $t $n   41 $t   $n $n $t  ,  $t $n $n $t   43 $n $t $t   )
    """.trimIndent(), "<fst>, snd")

    doTest("""
      $fooDeclaration
      foo($n   $n $t   $t$t $t $n   41 $t   $n $n $t  ,  $t $n $n $t   43 $n $t <caret> $t   )
    """.trimIndent(), "fst, <snd>")
  }

  fun testNoParameters() {
    doTest("""
      foo <- function() { 42 }
      
      foo(<caret>)
    """.trimIndent(), RBundle.message("parameter.info.no.parameters").replace("<", "&lt;").replace(">", "&gt;"))
  }

  fun testDots() {
    val fooDeclaration = """
      foo <- function(fst, snd, ..., thd) { 42 }
    """.trimIndent()

    doTest("""
      $fooDeclaration
      foo(41, 42, 4<caret>3, 44, 45, thd = 46)
    """.trimIndent(), "fst, snd, <...>, thd")

    doTest("""
      $fooDeclaration
      foo(41, 42, 43, 44, 4<caret>5, thd = 46)
    """.trimIndent(), "fst, snd, <...>, thd")

    doTest("""
      $fooDeclaration
      foo(41, 42, 43, 44, 45, thd =<caret> 46)
    """.trimIndent(), "fst, snd, ..., <thd>")
  }

  fun testDotsWithPermutation() {
    val fooDeclaration = """
      foo <- function(fst, snd, ..., thd) { 42 }
    """.trimIndent()

    doTest("""
      $fooDeclaration
      foo(41, 42, 4<caret>3, thd = 46, 44)
    """.trimIndent(), "fst, snd, <...>, thd, [...]")

    doTest("""
      $fooDeclaration
      foo(41, 42, 43, thd = 4<caret>6, 44)
    """.trimIndent(), "fst, snd, ..., <thd>, [...]")

    doTest("""
      $fooDeclaration
      foo(41, 42, 43, thd = 46, 4<caret>4)
    """.trimIndent(), "fst, snd, ..., thd, <[...]>")
  }

  fun testCallInCall() {
    val fooBarDeclaration = """
      foo <- function(fst, snd, thd) { 42 }
      bar <- function(aaa, bbb, ccc) { 44 }
    """.trimIndent()

    doTest("""
      $fooBarDeclaration
      foo(13, bar(1<caret>0, 9, 8), "aba")
    """.trimIndent(), "<aaa>, bbb, ccc")

    doTest("""
      $fooBarDeclaration
      foo(13, bar(10, 9, 8), "a<caret>ba")
    """.trimIndent(), "fst, snd, <thd>")

    doTest("""
      $fooBarDeclaration
      foo(13, b<caret>ar(10, 9, 8), "aba")
    """.trimIndent(), "fst, <snd>, thd")
  }

  fun testNotCompletedValue() {
    doTest("""
      foo <- function(fst, snd, thd) { 42 }
      foo(12, thd = <caret>)
    """.trimIndent(), "fst, <[thd]>, [snd]")
  }

  fun testMultiVariant() {
    doTest("""
      filter(10, 15, 43<caret>)
    """.trimIndent(),
           "x, filter, <method = c(\"convolution\", \"recursive\")>, sides = 2, circular = FALSE, init = NULL",
           ".data, <...>, .preserve = FALSE")
  }

  fun testDisable() {
    val fooDeclaration = "foo <- function(aa, bb) { 42 }"
    val result = "aa, bb"

    doTest("""
      $fooDeclaration
      foo(<caret>aa = 10, aa = 20)
    """.trimIndent(), result, isDisabled = true)

    doTest("""
      $fooDeclaration
      foo(aa = 10, cc = 20<caret>)
    """.trimIndent(), result, isDisabled = true)

    doTest("""
      $fooDeclaration
      foo(aa = 10, <caret>20, 30)
    """.trimIndent(), result, isDisabled = true)

  }

  fun doTest(text: String, vararg expectedResults: String, isDisabled: Boolean = false) {
    val hintFixture = EditorHintFixture(myFixture.testRootDisposable)

    myFixture.configureByText("a.R", text)
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SHOW_PARAMETER_INFO)
    UIUtil.dispatchAllInvocationEvents()

    val expectedVariants = wrap(*expectedResults, isDisabled = isDisabled).split('-').map { it.trim() }.sorted()
    val actualVariants = hintFixture.currentHintText?.split('-')?.map { it.trim() }?.sorted()
    assertEquals(expectedVariants, actualVariants)
  }

  companion object {
    const val t = "\t"
    const val n = "\n"

    fun wrap(vararg results: String, isDisabled: Boolean = false): String {
      return results.joinToString("\n-\n") { result ->
        val replacedQuote = result.replace("\"", "&quot;")
        val replacedHighlight = if (isDisabled) "<font color=a8a8a8>$replacedQuote</font>"
        else Regex("<(.+)>").replace(replacedQuote) { "<b>${it.groupValues[1]}</b>" }
        "<html>$replacedHighlight</html>"
      }
    }
  }
}