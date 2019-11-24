/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class RedundantSemicolonInspectionTest : RInspectionTest() {

  fun testSingleExpression() {
    doReplacementTest("42 + 42;", "42 + 42")
  }

  fun testSpaces() {
    doReplacementTest("42 +     \t \t 42;    \t \t     ", "42 +     \t \t 42    \t \t     ")
  }

  fun testComment() {
    doReplacementTest("42 + 42; # Some comment", "42 + 42 # Some comment")
  }

  fun testTwoExpressions() {
    doTest()
  }

  fun testManyLines() {
    doTest()
  }

  fun testInsideFunction() {
    doTest()
  }

  fun testInsideIfForStatements() {
    doTest()
  }

  override val inspection = RedundantSemicolonInspection::class.java
}