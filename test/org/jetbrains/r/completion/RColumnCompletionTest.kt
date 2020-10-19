package org.jetbrains.r.completion

import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

open class RColumnCompletionTest: RProcessHandlerBaseTestCase() {
  protected fun checkStaticCompletion(text: String, expectedToBePresent: List<String>, expectedToBeMissed: List<String>) {
    myFixture.configureByText("a.R", text)
    addRuntimeInfo()
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }.filter { it != "table" }
    for (completionElement in expectedToBePresent) {
      assertEquals(1, lookupStrings.count { it == completionElement })
    }
    for (completionElement in expectedToBeMissed) {
      assertEquals(0, lookupStrings.count { it == completionElement })
    }
  }

  protected fun addRuntimeInfo() {
    myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
  }
}