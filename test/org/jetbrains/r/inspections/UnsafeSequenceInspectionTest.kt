/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class UnsafeSequenceInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testLength() {
    doReplacementTest("a <- 1:length(c(1, 2, 3))", "a <- seq_along(c(1, 2, 3))")
  }

  fun testTable() {
    doReplacementTest("df <- 1:nrow(data.frame(aa = 1:5, bb = -1:-5))", "df <- seq_len(nrow(data.frame(aa = 1:5, bb = -1:-5)))")
    doReplacementTest("df <- 1:ncol(data.frame(aa = 1:5, bb = -1:-5))", "df <- seq_len(ncol(data.frame(aa = 1:5, bb = -1:-5)))")
    doReplacementTest("df <- 1:NROW(data.frame(aa = 1:5, bb = -1:-5))", "df <- seq_len(NROW(data.frame(aa = 1:5, bb = -1:-5)))")
    doReplacementTest("df <- 1:NCOL(data.frame(aa = 1:5, bb = -1:-5))", "df <- seq_len(NCOL(data.frame(aa = 1:5, bb = -1:-5)))")
  }

  fun testInFor() {
    doReplacementTest("for (i in 1:length(a)) {}", "for (i in seq_along(a)) {}")
  }

  fun testNoWarnings() {
    doReplacementTest("""
      # non-call right side 
      a <- 1:5
      b <- 1:-5
      
      # non-one left side
      a <- 0:length(a)
      
      # non-unsafe fun
      a <- 1:dim(x)
    """.trimIndent())
  }

  fun testQualifiedName() {
    doReplacementTest("1:base::length(a)", "seq_along(a)")
    doReplacementTest("1:base::ncol(a)", "seq_len(base::ncol(a))")
  }

  fun testOverridden() {
    doTest()
  }

  override val inspection = UnsafeSequenceInspection::class.java
}