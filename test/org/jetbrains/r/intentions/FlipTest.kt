/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

class FlipTest : AbstractRIntentionTest() {
  override val intentionName: String = "Flip ','"

  fun testSimpleFlip() {
    doExprTest("foo(a<caret>, b)", "foo(b<caret>, a)")
  }

  fun testMultilineFlip() {
    doExprTest("""
      foo(parameter1<caret>,
          parameter2())      
    """, """
      foo(parameter2()<caret>,
          parameter1)      
    """)
  }

  fun testDocumentationParam() {
    doExprTest("""
      #' @param x<caret>,y A Params
    """, """
      #' @param y<caret>,x A Params
    """)
  }
}
