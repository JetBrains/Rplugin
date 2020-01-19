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
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

class RSkeletonResolveTest : RConsoleBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  //fun testResolveDplyrFilter() {
    //val rValue = runTest("dplyr::fil<caret>ter()")
    //UsefulTestCase.assertInstanceOf(rValue, RValueFunction::class.java)
    //TestCase.assertTrue((rValue as RValueFunction).code.contains("UseMethod(\"filter\")"))
  //}

  fun testResolveDataset() {
    val rValue = runTest("iri<caret>s")
    UsefulTestCase.assertInstanceOf(rValue, RValueDataFrame::class.java)
    TestCase.assertEquals(5, (rValue as RValueDataFrame).cols)
  }

  // TODO: Rewrite test using new mechanisms for getting function code
  /*fun testResolveDplyrInternal() {
    val resolveResult = resolve("dplyr::any_<caret>exprs")
    TestCase.assertTrue(resolveResult is RSkeletonAssignmentStatement)
    val assignment = resolveResult as RSkeletonAssignmentStatement
    TestCase.assertFalse(assignment.stub.exported)
    val rValue = assignment.createRVar(console).value
    UsefulTestCase.assertInstanceOf(rValue, RValueFunction::class.java)
    TestCase.assertTrue((rValue as RValueFunction).code.contains("quote(`||`)"))
  }*/

  fun testResolveFilter() {
    val filterStats = resolve("fil<caret>ter()")
    TestCase.assertNotNull(filterStats)
    TestCase.assertEquals("stats", RPackage.getOrCreateRPackageBySkeletonFile(filterStats!!.containingFile)?.name)
    myFixture.file.runtimeInfo?.loadPackage("dplyr")
    val filterDplyr = resolve("fil<caret>ter()")
    TestCase.assertNotNull(filterDplyr)
    TestCase.assertEquals("dplyr", RPackage.getOrCreateRPackageBySkeletonFile(filterDplyr!!.containingFile)?.name)
  }

  fun testDoNotResolveToSkeletons() {
    val resolveResult = resolve("""
      conflicts <- function(pkg) 1
      make.conflicts <- function()
          conf<caret>licts('pkg')
    """.trimIndent())
    TestCase.assertEquals(myFixture.file, resolveResult?.containingFile)
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