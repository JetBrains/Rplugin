/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

class R6FindUsagesTest : FindUsagesTestBase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testR6ClassFieldOutOfClassUsage() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, someMethod = function(x = 1) { print(x) } ))
        obj <- MyClass${'$'}new()
        obj${'$'}someField
        obj${'$'}<caret>someField
    """, """
      <root> (2)
       Variable
        someField = 0
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            3obj${"$"}someField
            4obj${"$"}someField
    """)
  }

  fun testR6ClassFunctionOutOfClassUsage() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, someMethod = function(x = 1) { print(x) } ))
        obj <- MyClass${'$'}new()
        obj${'$'}<caret>someMethod()
        obj${'$'}someField
    """, """
      <root> (1)
       Variable
        someMethod = function(x = 1) { print(x) }
       Usages in Project Files (1)
        Unclassified (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            3obj${"$"}someMethod()
    """)
  }

  fun testR6ClassFieldOutOfClassChainedUsage() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0, someMethod = function(x = 1) { print(x) } ))
        obj <- MyClass${'$'}new()
        obj${'$'}someField
        obj${'$'}someMethod()${'$'}<caret>someField
    """, """
      <root> (2)
       Variable
        someField = 0
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            3obj${'$'}someField
            4obj${'$'}someMethod()${'$'}someField
    """)
  }

  fun testR6ClassInsideMethodFieldUsage(){
    doTest("""
      Accumulator <- R6Class("Accumulator", list(
        sum = 0,
        add = function(x = 1) {
          self${"$"}<caret>sum <- self${"$"}sum + x
          invisible(self)
        })
      )

      x <- Accumulator${"$"}new()
      x${"$"}add(4)${"$"}sum
      x${"$"}sum
    """, """
      <root> (3)
       Variable
        sum = 0
       Usages in Project Files (3)
        Unclassified (3)
         light_idea_test_case (3)
           (3)
           test.R (3)
            4self${'$'}sum <- self${'$'}sum + x
            10x${'$'}add(4)${'$'}sum
            11x${'$'}sum
    """)
  }

  fun testR6ClassFieldFromDefinition() {
    doTest("""
      MyClass <- R6Class("MyClass", list( <caret>someField = 0, someMethod = function(x = 1) { print(x) } ))
        obj <- MyClass${'$'}new()
        obj${'$'}someField
        obj${'$'}someField
    """, """
      <root> (2)
       Variable
        someField
       Usages in Project Files (2)
        Unclassified (2)
         light_idea_test_case (2)
           (2)
           test.R (2)
            3obj${"$"}someField
            4obj${"$"}someField
    """)
  }

  fun testR6ClassFieldWithSuperClass() {
    doTest("""
      ParentClass <- R6Class("ParentClass", list( someField = 0 ))
      ChildClass <- R6Class("ChildClass", inherit = ParentClass, list( add = function(x = 1) { print(x) } ))
      obj <- ChildClass${'$'}new()
      obj${'$'}<caret>someField
    """, """
      <root> (1)
       Variable
        someField = 0
       Usages in Project Files (1)
        Unclassified (1)
         light_idea_test_case (1)
           (1)
           test.R (1)
            4obj${'$'}someField
    """)
  }
}