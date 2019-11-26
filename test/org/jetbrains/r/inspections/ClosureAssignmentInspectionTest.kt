/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import icons.org.jetbrains.r.RBundle

class ClosureAssignmentInspectionTest : RInspectionTest() {

  fun testVariable() {
    doExprTest("a $LEFT 10", true)
  }

  fun testFunction() {
    doExprTest("f $LEFT function() { 42 }", true)
  }

  fun testRight() {
    doExprTest("42 $RIGHT a", true)
  }

  fun testChain() {
    doExprTest("a $LEFT b $LEFT c <- 42", true)
  }

  fun testSemicolon() {
    doExprTest("a $LEFT 42; b $LEFT c <- 42", true)
  }

  fun testNoWarnings() {
    doReplacementTest("""
      # variable 
      a <- 42
      
      # function 
      f <- function() {
        b <- 43
      }
      
      # named argument 
      print("a", quote = F)
    """.trimIndent())
  }

  override val inspection = ClosureAssignmentInspection::class.java

  companion object {
    private fun msg(operator: String) = RBundle.message("inspection.closure.assignment.description", operator)

    private val LEFT = "<weak_warning descr=\"${msg("<<-")}\"><<-</weak_warning>"
    private val RIGHT = "<weak_warning descr=\"${msg("->>")}\">->></weak_warning>"
  }
}