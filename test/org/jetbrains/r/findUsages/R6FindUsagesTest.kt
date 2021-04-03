/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

class R6FindUsagesTest : FindUsagesTestBase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testR6ClassFieldFromUsage() {
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

  fun testR6ClassFunctionFromUsage() {
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
        someField = 0
       Found usages (2 usages)
        Unclassified (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
           3obj${"$"}someField
           4obj${"$"}someField
    """)
  }
}