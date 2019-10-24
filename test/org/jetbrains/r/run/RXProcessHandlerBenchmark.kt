/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import junit.framework.TestCase
import org.junit.Ignore

@Ignore
class RXProcessHandlerBenchmark : RProcessHandlerBaseTestCase() {
  fun test() {
    val start = System.currentTimeMillis()
    for (i in 0 until N) {
      TestCase.assertEquals("[1] $i", rInterop.executeCode("$i", true).stdout.trim())
    }
    System.err.println("N=$N, ${System.currentTimeMillis() - start}ms")
  }

  companion object {
    const val N = 1000
  }
}
