/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

class RCommonFindUsagesTest  : FindUsagesTestBase() {
  fun testLocalVariable() {
    doTest("""
      my.local.<caret>variable <- 10
      print(my.local.variable)
      print("hello")
      some.function <- function() {
        print(my.local.variable + 20)
      }
    """, """
      <root> (2)
       Variable
        my.local.variable
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            2print(my.local.variable)
            5print(my.local.variable + 20)
    """)
  }

  //TODO: Fix R-334
  fun testLocalFunction() {
    doTest("""
      my.local.<caret>function <- function(x, y) x + y
      print(my.local.function(2, 3))
      print("hello")
      some.other.function <- function() {
        print(my.local.function(4, 5))
      }
    """, """
      <root> (2)
       Function
        my.local.function
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            2print(my.local.function(2, 3))
            5print(my.local.function(4, 5))
     """)
  }

  fun testLibraryFunction() {
    doTest("""
      base.package <- packageDescription("base")      
      dplyr.package <- package<caret>Description("dplyr")      
    """, """
      <root> (2)
       Function
        packageDescription(pkg, lib.loc = NULL, fields = NULL, drop = TRUE, encoding = "")
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            1base.package <- packageDescription("base")      
            2dplyr.package <- packageDescription("dplyr")
     """)
  }

  fun testParameter() {
    doTest("""
      func <- function(x<caret>, y, z) {
        x + y + z
      }
      
      x <- 15
      p <- x + 10
      func(x = p)
    """, """
      <root> (2)
       Function parameter
        x
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            2x + y + z
            7func(x = p)
     """)
  }

  fun testRoxygenParameter() {
    doTest("""
      #' Title
      #' 
      #' Description
      #'
      #' @param x, y X and y
      #' @param z Z
      #' @example
      #' #' @param x,y,z Params
      func <- function(x<caret>, y, z) {
        x + y + z
      }
    """, """
      <root> (2)
       Function parameter
        x
       Usages in Project Files (2)
        Unclassified (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            10x + y + z
        Usage in roxygen2 documentation (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            5#' @param x, y X and y
     """)
  }

  fun testRoxygenHelpPageLink() {
    doTest("""
      #' Title
      #' 
      #' Description
      #'
      #' @see [bar]
      #' [bar][baz]
      #' [bar](baz)
      #' <bar:bar>
      func <- function(x, y, z) {
        bar(x) + y + z
      }
      
      ba<caret>r <- function(x) { x + 42 }
    """, """
      <root> (2)
       Function
        bar
       Usages in Project Files (2)
        Unclassified (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            10bar(x) + y + z
        Usage in roxygen2 documentation (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            5#' @see [bar]
     """)
  }

  fun testS4Class() {
    doTest("""
      setClass('MyCl<caret>ass', slots = c(slot = 'numeric'))
      setClass('MyClass1', contains = 'MyClass')
      
      obj <- new('MyClass', slot = 5)
      obj1 <- new('MyClass1', slot = 6)
    """, """
      <root> (2)
       S4 class
        MyClass
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            2setClass('MyClass1', contains = 'MyClass')
            4obj <- new('MyClass', slot = 5)
     """)
  }

  fun testS4Slot() {
    doTest("""
      setClass('MyClass', slots = c(s<caret>lot = 'numeric', slot1 = 'character'))
      setClass('MyClass1', contains = 'MyClass')
      
      obj <- new('MyClass', slot = 5, slot1 = 'hello')
      obj1 <- new('MyClass1', slot = 6, slot1 = 'world')
      obj@slot
      obj@slot1
      obj1@slot
      obj1@slot1
    """, """
      <root> (4)
       S4 slot
        slot
       Usages in Project Files (4)
        Unclassified (4)
         light_idea_test_case (4)
           (4)
           test.R (4)
            4obj <- new('MyClass', slot = 5, slot1 = 'hello')
            5obj1 <- new('MyClass1', slot = 6, slot1 = 'world')
            6obj@slot
            8obj1@slot
     """)
  }

  fun testS4ComplexSlot() {
    doTest("""
      setClass('MyClass', slots = c(s<caret>lot = c('numeric', ext = 'character')))
      setClass('MyClass1', contains = 'MyClass')
      
      obj <- new('MyClass', slot1 = 5, slot.ext = 'hello')
      obj1 <- new('MyClass1', slot.ext = 'world', slot1 = 6)
      obj@slot
      obj@slot1
      obj@slot.ext
      obj1@slot
      obj1@slot1
      obj1@slot.ext
    """, """
      <root> (6)
       S4 slot
        slot
       Usages in Project Files (6)
        Unclassified (6)
         light_idea_test_case (6)
           (6)
           test.R (6)
            4obj <- new('MyClass', slot1 = 5, slot.ext = 'hello')
            5obj1 <- new('MyClass1', slot.ext = 'world', slot1 = 6)
            7obj@slot1
            8obj@slot.ext
            10obj1@slot1
            11obj1@slot.ext
     """)
  }
}