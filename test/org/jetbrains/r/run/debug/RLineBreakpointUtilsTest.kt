// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase

class RLineBreakpointUtilsTest : RUsefulTestCase() {
  fun testNotRFile() {
    val file = myFixture.configureByText("script.s", "print(\"ok\")").virtualFile
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testWhitespaces() {
    val file = myFixture.configureByText("script.r", "   ").virtualFile
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testComment() {
    val file = myFixture.configureByText("script.r", "# comment").virtualFile
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testLeftBrace() {
    val file = myFixture.configureByText("script.r", "{").virtualFile
    TestCase.assertTrue(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testRightBrace() {
    val file = myFixture.configureByText("script.r", "}").virtualFile
    TestCase.assertFalse(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testOk() {
    val file = myFixture.configureByText("script.r", "print('ok')").virtualFile
    TestCase.assertTrue(RLineBreakpointUtils.canPutAt(project, file, 0))
  }

  fun testBlock() {
    val file = myFixture.configureByText("script.r", """
      f = function()
      {
        # abc
        return(5)
      }
      
      a <- f()
      if (a > 5) {
        print("YES")
      } else
      {
        print("NO")
      }
    """.trimIndent()).virtualFile
    TestCase.assertEquals(listOf(true, false, false, true, false, false, true, true, true, false, false, true, false),
                          (0 until 13).map { RLineBreakpointUtils.canPutAt(project, file, it) })
  }
}