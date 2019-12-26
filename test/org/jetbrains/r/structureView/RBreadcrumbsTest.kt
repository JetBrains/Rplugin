/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.structureView

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.r.RUsefulTestCase

class RBreadcrumbsTest : RUsefulTestCase() {
  fun testBreadcrumbs() {
    myFixture.configureByText("test.R", """
      #<caret> Header 1 ----
      
      <caret>fun1 <- function() {
      }
      
      noCaretHere <- function() {
      }
      <caret>
      fun2 <- function () {
        foo()<caret>
      }
    """.trimIndent())

    val caretModel = myFixture.editor.caretModel

    val offsets = caretModel.allCarets.map { it.offset }

    val actual = offsets.joinToString(separator = "\n") { offset ->
      caretModel.moveToOffset(offset)
      val breadcrumbs = myFixture.breadcrumbsAtCaret
      breadcrumbs.joinToString(separator = "/") { it.text }
    }

    UsefulTestCase.assertSameLines("""
      test.R/Header 1
      test.R/fun1
      test.R
      test.R/fun2
    """.trimIndent(), actual)
  }
}