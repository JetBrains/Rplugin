/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RuntimeCompletionTests : RProcessHandlerBaseTestCase() {

  fun testLocalVariablesCompletion() {
    rInterop.executeCode("xxxx1 <- 10; xxxx2 <- 20", true)
    myFixture.configureByText("foo.R", "xxxx<caret>")
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    checkCompletion(listOf("xxxx1", "xxxx2"))
  }

  fun testDatasetCompletion() {
    rInterop.executeCode("some_symbol <- 10", true) // this identifier should not be in completion after `$`
    myFixture.configureByText("foo.R", "freeny$<caret>")
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    checkCompletion(listOf("income.level", "lag.quarterly.revenue", "market.potential", "price.index", "y"))
  }

  fun testFunctionCompletion() {
    rInterop.executeCode("ffff1 <- function(x, y = 10) return(x + y)", true)
    rInterop.executeCode("ffff2 <- function() return(1)", true)
    myFixture.configureByText("foo.R", "ffff<caret>")
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    TestCase.assertEquals(2, result.size)

    result[0].let {
      TestCase.assertEquals("ffff1", it.lookupString)
      val presentation = LookupElementPresentation()
      it.renderElement(presentation)
      TestCase.assertEquals("(x, y = 10)", presentation.tailText)
    }
    result[1].let {
      TestCase.assertEquals("ffff2", it.lookupString)
      val presentation = LookupElementPresentation()
      it.renderElement(presentation)
      TestCase.assertEquals("()", presentation.tailText)
    }
  }

  private fun checkCompletion(expected: List<String>) {
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    TestCase.assertEquals(expected, lookupStrings)
  }
}