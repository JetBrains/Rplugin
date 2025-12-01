/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.codeInsight.hints.getLanguageForSettingKey
import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.rmarkdown.RMarkdownFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

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
      
      foo(<hint text="a:"/>42, d = 40, <hint text="b:"/>33, bb = 43, aa = 20, aa = 15, <hint text="...("/>15, 20<hint text=")"/>)
    """.trimIndent())
  }

  fun testWrappedDots() {
    doParameterNameTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="a:"/>42, <hint text="b:"/>33, bb = 43, aa = 20, aa = 15, <hint text="...("/>15, 20<hint text=")"/>, d = 40)
    """.trimIndent())
  }

  fun testUnwrappedDots() {
    doParameterNameWithoutDotsWrapTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(<hint text="a:"/>42, <hint text="b:"/>33, bb = 43, aa = 20, aa = 15, <hint text="..."/>15, 20, d = 40)
    """.trimIndent())
  }

  fun testManyDotsBlocks() {
    doParameterNameTest("""
      foo <- function(a, b, ..., d) { a + b + d }
      
      foo(bb = 43, aa = 20, a = 42, aa = 15, b = 40, d = 40, <hint text="...("/>15, 20<hint text=")"/>)
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
          aa = 15, bb = 20, cc = 60,
          d = "wiwi")
    """.trimIndent())
  }

  fun testLibraryFunction() {
    doParameterNameTest("""
      data.table::data.table(aa = 100, keep.rownames = FALSE, aa = 20)
      stats::filter(1:100, rep(1, 3), <hint text="method:"/>"convolution", <hint text="sides:"/>2, <hint text="circular:"/>FALSE)
    """.trimIndent())
  }

  fun testIgnoreNamespace() {
    addIgnorePatterns("stats::*")

    doParameterNameTest("""
      stats::binomial("logit")
      stats::filter(1:100, rep(1, 3), "convolution", 2, FALSE)
      dplyr::filter(dplyr::tibble(), <hint text="...("/>aa == 12, bb == 13<hint text=")"/>)
    """.trimIndent())
  }

  fun testIgnoreFunctionFromNamespace() {
    addIgnorePatterns("stats::filter")

    doParameterNameTest("""
      stats::binomial(<hint text="link:"/>"logit")
      stats::filter(1:100, rep(1, 3), "convolution", 2, FALSE)
      dplyr::filter(dplyr::tibble(), <hint text="...("/>aa == 12, bb == 13<hint text=")"/>)
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

  fun testFirstDotsArguments() {
    doParameterNameTest("""
      foo <- function(..., x, y) { }
      
      foo(10, 20, x = 10, y = 15, <hint text="...("/>30, 40<hint text=")"/>)
      foo(xx = 10, yy = 20, x = 10, y = 15, <hint text="...("/>30, 40<hint text=")"/>)
    """.trimIndent())
  }

  fun testSingleArgInDots() {
    doParameterNameTest("""
      foo <- function(x, ..., y) { }
      
      foo(<hint text="x:"/>10, 20, y = 15)
      foo(<hint text="x:"/>10, xx = 20, y = 15)
    """.trimIndent())
  }

  fun testParameterNamesInRmd() {
    doParameterNameTest("""
      ```{r}
      foo <- function(a, b, c, d) {
        a + b + c + d
      }
      
      bar <- function() {}
      
      foo(<hint text="a:"/>10, <hint text="b:"/>20, d = 10, <hint text="c:"/>30)
      ```
      
      ```{python}
      def foo(a, b, c):
        return 42 + 42
      foo(50, 20, 40)
      ```
      
      ```{r}
      foo(<hint text="b:"/>30, <hint text="c:"/>20, a = 10, <hint text="d:"/>10)
      ```
      
      ```{python}
      foo(10, 20, 40)
      bar()
      ```
    """.trimIndent(), true)
  }

  fun testDotsInRmd() {
    doParameterNameTest("""
      ```{r}
      foo <- function(a, b, ..., d) {
        print(a, d, ..., b)
      }
      ```
      
      ```{r}
      foo(<hint text="a:"/>10, <hint text="b:"/>Inf, <hint text="...("/>"abacaba", NULL<hint text=")"/>, 
          d = 100, <hint text="...("/>foo(<hint text="a:"/>10, <hint text="b:"/>20, 30), NaN<hint text=")"/>)
      ```
    """.trimIndent(), true)
  }

  fun testDotsWithoutWrappingInRmd() {
    doParameterNameWithoutDotsWrapTest("""
      ```{r}
      foo <- function(a, b, ..., d) {
        print(a, d, ..., b)
      }
      ```
      
      ```{r}
      foo(<hint text="a:"/>10, <hint text="b:"/>Inf, <hint text="..."/>"abacaba", NULL, 
          d = 100, <hint text="..."/>foo(<hint text="a:"/>10, <hint text="b:"/>20, 30), NaN)
      ```
    """.trimIndent(), true)
  }

  fun testPipeOperator() {
    doParameterNameTest("""
      foo <- function (a, b, c) {
        print(c)
      }

      10 %>% foo(<hint text="b:"/>30, <hint text="c:"/>40)
      42 %>% foo(<hint text="b:"/>10, <hint text="c:"/>20) %>% foo(a = 10, <hint text="c:"/>40)
    """.trimIndent())
  }

  fun testPipeOperatorWithDots() {
    doParameterNameTest("""
      foo <- function (a, ..., c) {
        print(c)
      }

      10 %>% foo(c = 30, <hint text="...("/>40, 50<hint text=")"/>)
      42 %>% foo(40)
    """.trimIndent())
  }

  fun testFunctionFromSource() {
    myFixture.configureByFiles("hints/inlayFunctionFromSource/main.R",
                               "hints/inlayFunctionFromSource/A.R",
                               "hints/dummy.R")
    addLibraries()
    myFixture.testInlays()
  }

  fun testFunctionFrom2Source() {
    myFixture.configureByFiles("hints/inlayFunctionFrom2Source/main.R",
                               "hints/inlayFunctionFrom2Source/A.R",
                               "hints/inlayFunctionFrom2Source/B.R",
                               "hints/dummy.R")
    addLibraries()
    myFixture.testInlays()
  }

  private fun doParameterNameTest(text: String, isRmd: Boolean = false) {
    enableHints("R_HINT_OPTION_WRAP_DOTS")
    doTest(text, isRmd)
  }

  private fun doParameterNameWithoutDotsWrapTest(text: String, isRmd: Boolean = false) {
    enableHints()
    doTest(text, isRmd)
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

  private fun doTest(text: String, isRmd: Boolean) {
    val fileDotExtension = if (isRmd) "." + RMarkdownFileType.defaultExtension else RFileType.DOT_R_EXTENSION
    myFixture.configureByText("a$fileDotExtension", text)
    myFixture.testInlays()
  }
}