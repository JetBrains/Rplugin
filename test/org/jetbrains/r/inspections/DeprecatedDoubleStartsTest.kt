/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class DeprecatedDoubleStartsTest : RInspectionTest() {

  fun testDoubleStartsReplace() {
    doReplacementTest("a ** 10", "a ^ 10")
  }

  override val inspection = DeprecatedDoubleStarts::class.java
}