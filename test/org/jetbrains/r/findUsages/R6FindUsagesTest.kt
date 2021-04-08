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
      Usage (2 usages)
       Variable
        someField = 0
       Found usages (2 usages)
        Unclassified (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
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
      Usage (1 usage)
       Variable
        someMethod = function(x = 1) { print(x) }
       Found usages (1 usage)
        Unclassified (1 usage)
         light_idea_test_case (1 usage)
           (1 usage)
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
      Usage (2 usages)
       Variable
        someField = 0
       Found usages (2 usages)
        Unclassified (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
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
      Usage (3 usages)
       Variable
        sum = 0
       Found usages (3 usages)
        Unclassified (3 usages)
         light_idea_test_case (3 usages)
           (3 usages)
           4self${'$'}sum <- self${'$'}sum + x
           10x${'$'}add(4)${'$'}sum
           11x${'$'}sum
    """)
  }

  /// Not working due to picked target element is `list(...)` and not `someField`
  fun testR6ClassFieldFromDefinition() {
    doTest("""
      MyClass <- R6Class("MyClass", list( <caret>someField = 0, someMethod = function(x = 1) { print(x) } ))
        obj <- MyClass${'$'}new()
        obj${'$'}someField
        obj${'$'}someField
    """, """
      Usage (2 usages)
       Variable
        someField
       Found usages (2 usages)
        Unclassified (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
           3obj${"$"}someField
           4obj${"$"}someField
    """)
  }
}