/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.r.console.RConsoleRuntimeInfoImpl
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.addRuntimeInfo
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class R6ClassCompletionTest : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  private fun doWrongVariantsTest(text: String, vararg variants: String, withRuntimeInfo: Boolean = false, inConsole: Boolean = false) {
    val result = doTestBase(text, withRuntimeInfo, inConsole)
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    assertDoesntContain(lookupStrings, *variants)
  }

  private fun doTest(text: String,
                     vararg variants: Pair<String, String>, // <name, type>
                     strict: Boolean = true,
                     withRuntimeInfo: Boolean = false,
                     inConsole: Boolean = false) {
    val result = doTestBase(text, withRuntimeInfo, inConsole)
    assertNotNull(result)
    val lookupStrings = result.map {
      val elementPresentation = LookupElementPresentation()
      it.renderElement(elementPresentation)
      elementPresentation.itemText to elementPresentation.typeText
    }
    if (strict) {
      assertOrderedEquals(lookupStrings, *variants)
    }
    else {
      assertContainsOrdered(lookupStrings, *variants)
    }
  }

  private fun doTestBase(text: String, withRuntimeInfo: Boolean = false, inConsole: Boolean = false): Array<LookupElement> {
    myFixture.configureByText("foo.R", text)
    if (inConsole) {
      myFixture.file.putUserData(RConsoleView.IS_R_CONSOLE_KEY, true)
    }
    if (withRuntimeInfo) {
      myFixture.file.addRuntimeInfo(RConsoleRuntimeInfoImpl(rInterop))
    }
    return myFixture.completeBasic()
  }
}
