// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

class TtoTrueIntentionTest : AbstractRIntentionTest() {

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
  }

  @Throws(Throwable::class)
  fun testBooleanAssignment() {
    doExprTest("foo = <caret>T", "foo = TRUE<caret>")
  }

  @Throws(Throwable::class)
  fun testLoopCheck() {
    doTest()
  }

  override val intentionName: String = TtoTrueIntention().text
}
