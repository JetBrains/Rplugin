/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.completion.classes

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

  fun testUserClassWithSingleField() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0 ))
      obj <- MyClass${"$"}new()
      obj$<caret>
    """.trimIndent(), "someField" to "")
  }

  fun testUserClassWithSingleFieldChainedUsage() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0 ))
      obj <- MyClass${"$"}new()
      obj${'$'}someField${'$'}<caret>
    """.trimIndent(), "someField" to "")
  }

  fun testUserClassForInternalUsage() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, add = function(x = 1) {  self$<caret> } ))
    """.trimIndent(), "add" to "", "someField" to "")
  }

  fun testUserClassWithSeveralMembers() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, someMethod = function (x = 1) { print(x) } ))
      obj <- MyClass${"$"}new()
      obj$<caret>
    """.trimIndent(), "someField" to "", "someMethod" to "")
  }

  fun testUserClassWithFieldMethodActiveBinding() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, someMethod = function (x = 1) { print(x) }, random = function() { print('it is a random active binding') } ))
      obj <- MyClass${"$"}new()
      obj$<caret>
    """.trimIndent(), "random" to "", "someField" to "", "someMethod" to "")
  }

  fun testInheritedUserClassFieldsVisibility() {
    doTest("""
      ParentClass <- R6Class("ParentClass", list( someField = 0 ))
      ChildClass <- R6Class("ChildClass", inherit = ParentClass, list( add = function(x = 1) { print(x) } ))
      obj <- ChildClass${"$"}new()
      obj${'$'}someField${'$'}<caret>
    """.trimIndent(), "add" to "", "someField" to "")
  }

  fun testUserClassNameSuggestion() {
    doTest("""
      MyClass <- R6Class(<caret>)
    """.trimIndent(), "MyClass" to "string", containedInSuggestions = true)
  }

  private fun doWrongVariantsTest(text: String, vararg variants: String, withRuntimeInfo: Boolean = false, inConsole: Boolean = false) {
    val result = doTestBase(text, withRuntimeInfo, inConsole)
    assertNotNull(result)
    val lookupStrings = result.map { it.lookupString }
    assertDoesntContain(lookupStrings, *variants)
  }

  private fun doTest(text: String,
                     vararg variants: Pair<String, String>, // <name, type>
                     containedInSuggestions: Boolean = false,
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

    when {
      containedInSuggestions -> {
        variants.forEach { expectedSuggestion ->
          assert(lookupStrings.any { it.first == expectedSuggestion.first })
        }
      }
      strict -> {
        assertOrderedEquals(lookupStrings, *variants)
      }
      else -> {
        assertContainsOrdered(lookupStrings, *variants)
      }
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
