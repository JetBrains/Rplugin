/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

import com.intellij.testFramework.UsefulTestCase
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTargetUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RFindUsagesTest  : RProcessHandlerBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testLocalVariable() {
    doTest("""
      my.local.<caret>variable <- 10
      print(my.local.variable)
      print("hello")
      some.function <- function() {
        print(my.local.variable + 20)
      }
    """, """
      Usage (2 usages)
       Variable
        my.local.variable
       Found usages (2 usages)
        Unclassified usage (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
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
      Usage (2 usages)
       Variable
        my.local.function
       Found usages (2 usages)
        Unclassified usage (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
           2print(my.local.function(2, 3))
           5print(my.local.function(4, 5))
     """)
  }

  fun testLibraryFunction() {
    doTest("""
      base.package <- packageDescription("base")      
      dplyr.package <- package<caret>Description("dplyr")      
    """, """
      Usage (2 usages)
       Variable
        packageDescription(pkg, lib.loc = NULL, fields = NULL, drop = TRUE, encoding = "")
       Found usages (2 usages)
        Unclassified usage (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
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
      Usage (2 usages)
       Variable
        x
       Found usages (2 usages)
        Unclassified usage (2 usages)
         light_idea_test_case (2 usages)
           (2 usages)
           2x + y + z
           7func(x = p)
     """)
  }

  private fun doTest(@Language("R") code: String, expected: String) {
    myFixture.configureByText("test.R", code.trimIndent())
    val element = myFixture.elementAtCaret
    val targets = UsageTargetUtil.findUsageTargets(element)
    assertNotNull(targets)
    assertTrue(targets.size > 0)
    val target = (targets[0] as PsiElementUsageTarget).element
    val actual = myFixture.getUsageViewTreeTextRepresentation(target)
    UsefulTestCase.assertSameLines(expected.trimIndent(), actual)
  }
}