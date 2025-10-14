/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.pom.PomTargetPsiElement
import junit.framework.TestCase
import com.intellij.r.psi.RFileType
import org.jetbrains.r.console.RConsoleBaseTestCase
import com.intellij.r.psi.psi.DataFramePomTarget
import com.intellij.r.psi.psi.FunctionPomTarget
import com.intellij.r.psi.psi.VariablePomTarget
import kotlin.reflect.KClass

class GlobalResolveTest : RConsoleBaseTestCase() {
  fun testResolveVariable() {
    doTest("xyz <- 312312", "xy<caret>z", VariablePomTarget::class)
  }

  fun testResolveList() {
    doTest("xyz <- list(1,2,3,4)", "xy<caret>z", VariablePomTarget::class)
  }

  fun testResolveEnvironment() {
    doTest("xyz <- globalenv()", "xy<caret>z", VariablePomTarget::class)
  }

  fun testResolveFunction() {
    doTest("xyz <- function(x)x + 1", "xy<caret>z", FunctionPomTarget::class)
  }

  fun testResolveDataFrame() {
    doTest("xyz <- cars", "xy<caret>z", DataFramePomTarget::class)
  }

  private fun doTest(assignment: String, expression: String, expectedClass: KClass<*>) {
    rInterop.executeCode(assignment)
    myFixture.configureByText(RFileType, expression)
    val resolve = resolve()
    TestCase.assertEquals(1, resolve.size)
    val resolveResult = resolve[0].element
    TestCase.assertTrue(resolveResult is PomTargetPsiElement)
    TestCase.assertTrue(expectedClass.isInstance((resolveResult as PomTargetPsiElement).target))
  }

}