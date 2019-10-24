/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.inspections.dplyr.DplyrBooleanOperatorsInspection

class DplyrBooleanOperatorsInspectionTest : RInspectionTest() {
  fun testReplace() {
    doReplacementTest("filter(table, a && b)", "filter(table, a & b)")
    doReplacementTest("filter(table, a || b)", "filter(table, a | b)")
    doReplacementTest("table %>% filter(a && b)", "table %>% filter(a & b)")
    doReplacementTest("table %>% filter(a || b)", "table %>% filter(a | b)")
  }

  fun testNoReplace() {
    doReplacementTest("a && b")
    doReplacementTest("a || b")
    doReplacementTest("filter(a && b)")
    doReplacementTest("filter(a || b)")
  }

  override val inspection: Class<out RInspection>
    get() = DplyrBooleanOperatorsInspection::class.java
}
