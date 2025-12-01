/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.pom.PomTargetPsiElement
import com.intellij.r.psi.psi.RSkeletonParameterPomTarget
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RNamedArgumentResolveTest : RLightCodeInsightFixtureTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testNamedArgument() {
    doTest("xx", """
      my_func <- function(xx) {
      }

      my_func(x<caret>x = 21321)
    """.trimIndent())
  }

  fun testNamedDefaultArgument() {
    doTest("xxx = 321321", """
      my_func <- function(xxx = 321321) {

      }

      my_func(xx<caret>x = 32131534534)
    """.trimIndent())
  }

  fun testResolvePNG() {
    doTestSkeletonResolve("width", """
      png(file='wordcloud.png', bg='transparent', wi<caret>dth = 1200, height = 1600)
    """.trimIndent())
  }

  private fun doTest(resolveTargetParentText: String, text: String) {
    myFixture.configureByText("test.R", text)
    val results = resolve()
    assertEquals(results.size, 1)
    val element = results[0].element!!
    assertTrue(element.isValid)
    assertEquals(resolveTargetParentText, element.text)
  }

  private fun doTestSkeletonResolve(parameterName: String, text: String) {
    myFixture.configureByText("test.R", text)
    val results = resolve()
    assertEquals(results.size, 1)
    val element = results[0].element!!
    assertTrue(element.isValid)
    assertTrue(element is PomTargetPsiElement)
    val target = (element as PomTargetPsiElement).target
    assertTrue(target is RSkeletonParameterPomTarget)
  }
}
