/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class CompareToNaInspectionTest : org.jetbrains.r.inspections.RInspectionTest() {
  fun testEquals() {
    doReplacementTest("x == NA", "is.na(x)")
    doReplacementTest("NA == x", "is.na(x)")
    doReplacementTest("x + y + f(z) == NA", "is.na(x + y + f(z))")
  }

  fun testNotEquals() {
    doReplacementTest("x != NA", "!is.na(x)")
    doReplacementTest("NA != x", "!is.na(x)")
    doReplacementTest("x + y + f(z) != NA", "!is.na(x + y + f(z))")
  }

  override val inspection: Class<out RInspection>
    get() = CompareToNaInspection::class.java
}
