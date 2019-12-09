/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections
class NamingConventionInspectionTest : RInspectionTest() {

  fun testVariable() {
    doReplacementTest("..aB_Caa.Daba <- 42", "a_b_caa_daba <- 42")
  }

  fun testFunction() {
    doReplacementTest("..FunFun__.data.Fun <- function(par) { #smt }", "fun_fun.data.Fun <- function(par) { #smt }")
  }

  fun testForLoop() {
    doReplacementTest("""
      for (..AbA..Daba.. in 1:10) {
        ..AbA..Daba..
      }
    """.trimIndent(), """
      for (ab_a_daba in 1:10) {
        ab_a_daba
      }
    """.trimIndent())
  }

  fun testNoWarnings() {
    doReplacementTest("""
      # variable 
      my_variable <- 42
      
      # function 
      my_function_2020.data.Table <- function() {
        b <- 43
      }
      
      # operators
      `[.my_class` <- function() {}
      `[<-.my_class` <- function() {}
      `+.my_class` <- function() {}
      `$.my_class` <- function() {}
      `[[.my_class` <- function() {}
      `[[<-.my_class` <- function() {}
      `&.my_class` <- function() {}
      `|.my_class` <- function() {}
      `^.my_class` <- function() {}
    """.trimIndent())
  }

  fun testReassign() {
    doWeakTest()
  }

  override val inspection = NamingConventionInspection::class.java
}

