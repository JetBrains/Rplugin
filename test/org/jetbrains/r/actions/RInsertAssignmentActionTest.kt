/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.application.options.CodeStyle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase

class RInsertAssignmentActionTest : RUsefulTestCase() {
  fun testAddAroundSpaces() {
    myFixture.configureByText("test.R", """
      x<caret>
    """.trimIndent())
    doActionTest("""
      x <- <caret>
    """.trimIndent(), "RInsertAssignmentAction")
  }

  fun testPrefixSpaceExists() {
    myFixture.configureByText("test.R", """
      x <caret>
    """.trimIndent())
    doActionTest("""
      x <- <caret>
    """.trimIndent(), "RInsertAssignmentAction")
  }

  fun testNoSpaces() {
    myFixture.configureByText("test.R", """
      x<caret>
    """.trimIndent())
    val settings = CodeStyle.getSettings(myFixture.file).getCommonSettings(RLanguage.INSTANCE)
    assert(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS) // check current default
    try {
      settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false
      doActionTest("""
        x<-<caret>
      """.trimIndent(), "RInsertAssignmentAction")
    }
    finally {
      settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true
    }
  }

  fun testRMarkdown() {
    myFixture.configureByText("test.rmd", """
      ```
      x<caret>
      ```
    """.trimIndent())
    doActionTest("""
      ```
      x <- <caret>
      ```
    """.trimIndent(), "RInsertAssignmentAction")
  }

  fun testAnotherLanguage() {
    myFixture.configureByText("test.yml", """
      x <caret>
    """.trimIndent())
    // Nothing should be changed:
    doActionTest("""
      x <caret>
    """.trimIndent(), "RInsertAssignmentAction")
  }
}
