/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.codeInsight.hints.getLanguageForSettingKey
import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

@Suppress("UnstableApiUsage")
class RInlayParameterHintsProviderTest : RLightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testParameterNames() {
    doParameterNameTest("""
      foo <- function(a, b, c) { a + length(b) + c }
      
      foo(<hint text="a:"/>42, <hint text="b:"/>"abacaba", <hint text="c:"/>15)
    """.trimIndent())
  }

  fun testNamedArguments() {
    doParameterNameTest("""
      foo <- function(a, b, c) { a + length(b) + c }
      
      foo(<hint text="a:"/>42, b = "abacaba", <hint text="c:"/>15)
    """.trimIndent())
  }

  fun testConstants() {
    doParameterNameTest("""
      foo <- function(a, b, c, d, e, f, g, h, i, j, k, l1, l2, l3, l4, l5) { }
      
      foo(<hint text="a:"/>"abacaba", <hint text="b:"/>42, <hint text="c:"/>TRUE, <hint text="d:"/>F, <hint text="e:"/>NULL,
          <hint text="f:"/>NA, <hint text="g:"/>NA_complex_, <hint text="h:"/>Inf, <hint text="i:"/>NA, <hint text="j:"/>NaN, 
          function(a, b) { }, callll(), 1:100, 5 + 5, l5)
    """.trimIndent())
  }

  fun testPermutation() {
    doParameterNameTest("""
      foo <- function(a, b, c, d) { length(a) + b + c + d }
      
      foo(c = 42, <hint text="a:"/>"abacaba", b = 43, <hint text="d:"/>15)
    """.trimIndent())
  }

  fun testDots() {
    doParameterNameTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="a:"/>42, d = 40, <hint text="b:"/>33, <hint text="..."/>bb = 43, aa = 20, aa = 15, 15, 20)
    """.trimIndent())
  }

  fun testWrappedDots() {
    doParameterNameTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="a:"/>42, <hint text="b:"/>33, <hint text="...("/>bb = 43, aa = 20, aa = 15, 15, 20<hint text=")"/>, d = 40)
    """.trimIndent())
  }

  fun testUnwrappedDots() {
    doParameterNameWithoutDotsWrapTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="a:"/>42, <hint text="b:"/>33, <hint text="..."/>bb = 43, aa = 20, aa = 15, 15, 20, d = 40)
    """.trimIndent())
  }

  fun testManyDotsBlocks() {
    doParameterNameTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="...("/>bb = 43, aa = 20<hint text=")"/>, a = 42, <hint text="...("/>aa = 15<hint text=")"/>, b = 40, d = 40, <hint text="..."/>15, 20)
    """.trimIndent())
  }

  fun testLines() {
    doParameterNameTest("""
      foo <- function(a, foo, ..., d) { a + b + d }
      bar <- function(mode, len) { 100500 }
      
      foo(<hint text="a:"/>20,
          function() {
            42 + 43 + 44 + bar(<hint text="mode:"/>"normal", <hint text="len:"/>500)
          },
          <hint text="...("/>aa = 15, bb = 20, cc = 60<hint text=")"/>,
          d = "wiwi")
    """.trimIndent())
  }

  fun testLibraryFunction() {
    doParameterNameTest("""
      data.table::data.table(<hint text="...("/>aa = 100<hint text=")"/>, keep.rownames = FALSE, <hint text="..."/>aa = 20)
      stats::filter(1:100, rep(1, 3), <hint text="method:"/>"convolution", <hint text="sides:"/>2, <hint text="circular:"/>FALSE)
    """.trimIndent())
  }

  fun testIgnoreNamespace() {
    addIgnorePatterns("stats::*")

    doParameterNameTest("""
      stats::binomial("logit")
      stats::filter(1:100, rep(1, 3), "convolution", 2, FALSE)
      dplyr::filter(dplyr::tibble(), <hint text="..."/>aa == 12, bb == 13)
    """.trimIndent())
  }

  fun testIgnoreFunctionFromNamespace() {
    addIgnorePatterns("stats::filter")

    doParameterNameTest("""
      stats::binomial(<hint text="link:"/>"logit")
      stats::filter(1:100, rep(1, 3), "convolution", 2, FALSE)
      dplyr::filter(dplyr::tibble(), <hint text="..."/>aa == 12, bb == 13)
    """.trimIndent())
  }

  fun testIgnoreFunction() {
    addIgnorePatterns("*filter")

    doParameterNameTest("""
      stats::binomial(<hint text="link:"/>"logit")
      stats::filter(1:100, rep(1, 3), "convolution", 2, FALSE)
      dplyr::filter(dplyr::tibble(), aa == 12, bb == 13)
      dplyr::tally(dplyr::tibble(), <hint text="wt:"/>NULL)
    """.trimIndent())
  }

  fun testIgnoreFunctionsWithParameters() {
    addIgnorePatterns("(x, y)")

    doParameterNameTest("""
      foo <- function(x, y) { }
      bar <- function(x, y, z) { }
      
      foo(10, 20)
      bar(<hint text="x:"/>10, <hint text="y:"/>20, <hint text="z:"/>30)
    """.trimIndent())
  }

  private fun doParameterNameTest(text: String) {
    enableHints("R_HINT_OPTION_WRAP_DOTS")
    doTest(text)
  }

  private fun doParameterNameWithoutDotsWrapTest(text: String) {
    enableHints()
    doTest(text)
  }

  private fun enableHints(vararg names: String) {
    val allHints = RInlayParameterHintsProvider().supportedOptions
    allHints.forEach { if (it.id in names) it.set(true) else it.set(false) }
  }

  private fun addIgnorePatterns(vararg patterns: String) {
    patterns.forEach {
      ParameterNameHintsSettings.getInstance().addIgnorePattern(getLanguageForSettingKey(RLanguage.INSTANCE), it)
    }
  }

  private fun doTest(text: String) {
    myFixture.configureByText("a.R", text)
    myFixture.testInlays()
  }
}