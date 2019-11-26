/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class TripleColonToDoubleColonInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testBase() {
    doReplacementTest("base:::print(\"Hello world!\")", "base::print(\"Hello world!\")")
  }

  fun testDplyr() {
    doReplacementTest("dplyr:::filter(tibble(aa = 1:5))", "dplyr::filter(tibble(aa = 1:5))")
  }

  fun testVariable() {
    doReplacementTest("base:::T", "base::T")
  }

  fun testNoWarningForNonEmptySeparator() {
    doReplacementTest("""
      # not exported
      dplyr:::any_exprs()
      
      # double colon
      base::paste("a", "b")
      
      # not exists package
      blabla:::fun()
      
      # single colon
      a <- 1:5
    """.trimIndent())
  }

  override val inspection = TripleColonToDoubleColonInspection::class.java
}