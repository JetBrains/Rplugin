/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

class R6FindUsagesTest : FindUsagesTestBase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testR6ClassField() {
    doTest("""
      MyClass <- R6Class("MyClass", list( someField = 0 ))
      obj <- MyClass${'$'}new()
      obj${'$'}<caret>someField
    """, """
      Usage (2 usages)
       Variable
        my.local.variable
       Found usages (2 usages)
        Unclassified (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
           2print(my.local.variable)
           5print(my.local.variable + 20)
    """)
  }
}