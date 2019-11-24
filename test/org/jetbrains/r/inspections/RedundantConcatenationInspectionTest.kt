/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class RedundantConcatenationInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testEmptyArgumentList() {
    doReplacementTest("c()", "NULL")
  }

  fun testSingleArgument() {
    doReplacementTest("c(42)", "42")
  }

  fun testCallInside() {
    doReplacementTest("c(paste('a', 'b'))", "paste('a', 'b')")
  }

  fun testInnerCall() {
    doReplacementTest("paste(c('a'), 'a'))", "paste('a', 'a'))")
  }

  fun testQualifiedName() {
    doReplacementTest("base::c()", "NULL")
    doReplacementTest("base::c(42)", "42")
    doReplacementTest("paste(base::c(), '42')", "paste(NULL, '42')")
  }

  fun testNoWarnings() {
    doReplacementTest("""
      # more than 1 args
      c(10, 42)
      
      # named argument
      c(a = 10)
      c(10, b = 42)
      
      # dots 
      c(...)
      c(10, ...)
    """.trimIndent())
  }

  fun testOverriddenC() {
    doWeakTest()
  }

  override val inspection = RedundantConcatenationInspection::class.java
}