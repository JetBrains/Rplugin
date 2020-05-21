/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.testFramework.UsefulTestCase

class RSourceProgressInputFilterTests : UsefulTestCase() {

  fun testNormalOutput() {
    val progressInputFilter = RSourceProgressInputFilter { }
    assertNull(progressInputFilter.applyFilter ("abacaba", ConsoleViewContentType.NORMAL_OUTPUT))
    assertNull(progressInputFilter.applyFilter (">__jb_rplugin_progress__ statement __ 2\n", ConsoleViewContentType.NORMAL_OUTPUT))
  }

  fun testErrorOutput() {
    var text = "abacaba"
    var expected = ""
    val progressInputFilter = RSourceProgressInputFilter { assertEquals(expected, it) }
    assertEquals("abacaba", progressInputFilter.applyFilter (text, ConsoleViewContentType.ERROR_OUTPUT)?.get(0)?.first)
    text = ">__jb_rplugin_progress__ statement __ 2\n"
    expected = " statement __ 2"
    assertEquals("", progressInputFilter.applyFilter (text, ConsoleViewContentType.ERROR_OUTPUT)?.get(0)?.first)
    text = ">>__jb_rplugin_progress__ statement __ 2\n"
    assertEquals(">", progressInputFilter.applyFilter (text, ConsoleViewContentType.ERROR_OUTPUT)?.get(0)?.first)
    text = ">__jb_rplugin_progress_>__jb_rplugin_progress__ statement __ 3\n"
    expected = " statement __ 3"
    assertEquals(">__jb_rplugin_progress_", progressInputFilter.applyFilter (text, ConsoleViewContentType.ERROR_OUTPUT)?.get(0)?.first)
  }

  fun testChunks() {
    var text = ">>__jb_rplugin_progress_>__jb_rplugin_progress__ statement __ 2\nf"
    var expected = " statement __ 2"
    val progressInputFilter = RSourceProgressInputFilter { assertEquals(expected, it) }
    val chunks = text
      .mapNotNull { char -> progressInputFilter.applyFilter("" + char, ConsoleViewContentType.ERROR_OUTPUT)?.get(0) }
      .filter { it.first.isNotEmpty() }

    assertEquals(3, chunks.size)
    assertEquals(chunks[0].second, ConsoleViewContentType.ERROR_OUTPUT)
    assertEquals(">", chunks[0].first)
    assertEquals(">__jb_rplugin_progress_", chunks[1].first)
    assertEquals("f", chunks[2].first)
  }
}