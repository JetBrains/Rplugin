/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.packages.RPackage
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

  fun testResolveFilter() {
    val filterStats = resolve("fil<caret>ter()")
    TestCase.assertNotNull(filterStats)
    TestCase.assertEquals("stats", RPackage.getOrCreate(filterStats!!.containingFile)?.packageName)
    myFixture.file.runtimeInfo?.loadPackage("dplyr")
    val filterDplyr = resolve("fil<caret>ter()")
    TestCase.assertNotNull(filterDplyr)
    TestCase.assertEquals("dplyr", RPackage.getOrCreate(filterDplyr!!.containingFile)?.packageName)
  }

  private fun runTest(expression: String): RValue {
    val resolveResult = resolve(expression)
    TestCase.assertTrue(resolveResult is RSkeletonAssignmentStatement)
    return (resolveResult as RSkeletonAssignmentStatement).createRVar(console).value
  }

  private fun resolve(expression: String): PsiElement? {
    myFixture.configureByText(RFileType, expression)
    val resolve = resolve()
    TestCase.assertEquals(1, resolve.size)
    return resolve[0].element
  }

}