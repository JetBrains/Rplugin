/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class AssignmentInspectionTest : RInspectionTest() {

  fun testVariable() {
    doReplacementTest("a = 10", "a <- 10")
  }

  fun testFunction() {
    doReplacementTest("f = function(par) { #smt }", "f <- function(par) { #smt }")
  }

  fun testInsideFunction() {
    doReplacementTest("""
      f <- function() {
        a = 10
      }
    """.trimIndent(), """
      f <- function() {
        a <- 10
      }
    """.trimIndent())
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

  override val inspection = AssignmentInspection::class.java
}