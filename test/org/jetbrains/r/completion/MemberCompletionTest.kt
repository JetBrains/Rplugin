/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class MemberCompletionTest : RProcessHandlerBaseTestCase() {

  fun testQuotes() {
    val initializeString = """
      x <- new.env()
      x${'$'}`Foo Bar Baz` <- 123
      x${'$'}Foo <- "a"
    """.trimIndent()

    myFixture.configureByText("foo.R", "")
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    rInterop.executeCode(initializeString, true)

    doApplyCompletionTest("""
      $initializeString
      x${'$'}F<caret>
    """.trimIndent(), "Foo", """
      $initializeString
      x${'$'}Foo<caret>
    """.trimIndent(), true)

    doApplyCompletionTest("""
      $initializeString
      x${'$'}F<caret>
    """.trimIndent(), "Foo Bar Baz", """
      $initializeString
      x${'$'}`Foo Bar Baz`<caret>
    """.trimIndent(), true)
  }
}
