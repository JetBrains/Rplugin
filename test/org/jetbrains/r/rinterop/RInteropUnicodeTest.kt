/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import junit.framework.TestCase
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RInteropUnicodeTest : RProcessHandlerBaseTestCase() {
  fun testUnicode() {
    val s = "abc\u0448\u79c1 \u2203 x"
    rInterop.executeCode("var <- '$s'")
    TestCase.assertEquals("[1] \"$s\"", rInterop.executeCode("var").stdout.trim())
    TestCase.assertEquals(s, rInterop.executeCode("cat(var)").stdout.trim())
    TestCase.assertTrue(s in (RReference.expressionRef("var", rInterop).getValueInfo() as RValueSimple).text)
    TestCase.assertTrue(rInterop.globalEnvLoader.variables.any { it.name == "var" })
  }

  fun testUnicodeEscape() {
    val s = "abc\u0448\u79c1 \u2203 x"
    val escaped = "abc\\u0448\\u79c1 \\u2203 x"
    rInterop.executeCode("var <- '$escaped'")
    TestCase.assertEquals("[1] \"$s\"", rInterop.executeCode("var").stdout.trim())
    TestCase.assertEquals(s, rInterop.executeCode("cat(var)").stdout.trim())
    TestCase.assertTrue(s in (RReference.expressionRef("var", rInterop).getValueInfo() as RValueSimple).text)
    TestCase.assertTrue(rInterop.globalEnvLoader.variables.any { it.name == "var" })
  }

  fun testDataTable() {
    addLibraries()
    val s1 = "column \u79c1"
    val s2 = "value \u79c2"
    val table = rInterop.dataFrameGetViewer(RReference.expressionRef("dplyr::tibble('$s1' = '$s2')", rInterop)).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(s1, table.getColumnName(1))
    TestCase.assertEquals(s2, table.getValueAt(0, 1))
  }
}