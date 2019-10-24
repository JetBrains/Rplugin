/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.inspections.dplyr.DplyrCallToPipeInspection

class DplyrCallToPipeInspectionTest : RInspectionTest() {
  fun test() {
    doReplacementTest(
      "filter(summarise(group_by(table, y, z), x = sum(x)), a > 0)",
      "table %>% group_by(y, z) %>% summarise(x = sum(x)) %>% filter(a > 0)"
    )
  }

  fun testParenthesis() {
    doReplacementTest(
      "filter(summarise(group_by(table, y, z), x = sum(x)), a > 0) $ ff",
      "(table %>% group_by(y, z) %>% summarise(x = sum(x)) %>% filter(a > 0)) $ ff"
    )
  }

  fun testNoParenthesis() {
    doReplacementTest(
      "filter(summarise(group_by(table, y, z), x = sum(x)), a > 0) + ff",
      "table %>% group_by(y, z) %>% summarise(x = sum(x)) %>% filter(a > 0) + ff"
    )
  }

  override val inspection: Class<out RInspection>
    get() = DplyrCallToPipeInspection::class.java
}
