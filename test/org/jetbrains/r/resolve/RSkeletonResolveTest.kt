/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.rinterop.RValue
import org.jetbrains.r.rinterop.RValueDataFrame
import org.jetbrains.r.rinterop.RValueFunction
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

class RSkeletonResolveTest : RConsoleBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testResolveDplyrFilter() {
    val rValue = runTest("dplyr::fil<caret>ter()")
    UsefulTestCase.assertInstanceOf(rValue, RValueFunction::class.java)
    TestCase.assertTrue((rValue as RValueFunction).code.contains("UseMethod(\"filter\")"))
  }

  fun testResolveDataset() {
    val rValue = runTest("iri<caret>s")
    UsefulTestCase.assertInstanceOf(rValue, RValueDataFrame::class.java)
    TestCase.assertEquals(5, (rValue as RValueDataFrame).cols)
  }

  private fun runTest(expression: String): RValue {
    myFixture.configureByText(RFileType, expression)
    val resolve = resolve()
    TestCase.assertEquals(1, resolve.size)
    val resolveResult = resolve[0].element
    TestCase.assertTrue(resolveResult is RSkeletonAssignmentStatement)
    return (resolveResult as RSkeletonAssignmentStatement).createRVar(console).value
  }

}