/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import junit.framework.TestCase
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.psi.ReferenceKind
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.getKind

class IdentifierKindTest : RLightCodeInsightFixtureTestCase() {

  fun testLocalTopLevel() {
    doTest("""
      xxx <- 432432
      x<caret>xx
    """.trimIndent(), ReferenceKind.LOCAL_VARIABLE)
  }

  fun testFunction() {
    doTest("""
      function() {
        xxx <- 432432
        x<caret>xx
      }
    """.trimIndent(), ReferenceKind.LOCAL_VARIABLE)
  }

  fun testGlobal() {
    doTest("""
      x<caret>xx
    """.trimIndent(), ReferenceKind.OTHER)
  }

  fun testParameter() {
    doTest("""
      function(xxx) {
        xx<caret>x
      }
    """.trimIndent(), ReferenceKind.PARAMETER)
  }

  fun testToplevelAssign() {
    doTest("""
      x<caret>xx <- 432423
    """.trimIndent(), ReferenceKind.LOCAL_VARIABLE)
  }

  fun testFunctionAssign() {
    doTest("""
      function() {
         x<caret>xx <- 432423
      }
    """.trimIndent(), ReferenceKind.LOCAL_VARIABLE)
  }

  fun testClosure() {
    doTest("""
      function() {
         xxx <- 34432432
         function() {
            xx<caret>x
         }
      }
    """.trimIndent(), ReferenceKind.CLOSURE)
  }

  fun doTest(text: String, expectedKind: ReferenceKind) {
    myFixture.configureByText("test.R", text)
    val rIdentifier = findElementAtCaret(RIdentifierExpression::class.java)
    TestCase.assertEquals(expectedKind, rIdentifier?.getKind())
  }

}