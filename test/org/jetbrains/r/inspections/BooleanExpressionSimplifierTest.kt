/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class BooleanExpressionSimplifierTest : RInspectionTest() {

  fun testNoAction() {
    doReplacementTest("x")
    doReplacementTest("x && y")
    doReplacementTest("x && y && z")
    doReplacementTest("x && (y || !z)")
  }

  fun testSimplifyWithConstants() {
    doReplacementTest("FALSE && x", "FALSE")
    doReplacementTest("x && FALSE", "FALSE")
    doReplacementTest("TRUE && x", "x")
    doReplacementTest("x && TRUE", "x")
    doReplacementTest("FALSE || x", "x")
    doReplacementTest("x || FALSE", "x")
    doReplacementTest("TRUE || x", "TRUE")
    doReplacementTest("x || TRUE", "TRUE")
    doReplacementTest("!TRUE", "FALSE")
    doReplacementTest("!FALSE", "TRUE")
    doReplacementTest("TRUE && !(TRUE || !FALSE)", "FALSE")
  }

  fun testCantBeDeleted() {
    doReplacementTest("f() && FALSE")
    doReplacementTest("FALSE && f()", "FALSE")
    doReplacementTest("f() || TRUE")
    doReplacementTest("TRUE || f()", "TRUE")
    doReplacementTest("f() && FALSE && g()", "f() && FALSE")

    doReplacementTest("FALSE & f()")
    doReplacementTest("TRUE | f()")
  }

  fun testDoubleNegation() {
    doReplacementTest("!!x", "x")
    doReplacementTest("!!!x", "!x")
    doReplacementTest("!!f()", "f()")
    doReplacementTest("!((TRUE && !x))", "x")
  }

  fun testParenthesis() {
    doReplacementTest("x && (y && z)")
    doReplacementTest("(x && y) && z")
    doReplacementTest("(x || y) && z")
    doReplacementTest("(x && y) || z")

    doReplacementTest("(!!x && y) || z", "x && y || z")
    doReplacementTest("(!!x || y) && z", "(x || y) && z")

    doReplacementTest("!!x && (y && z)", "x && y && z")
    doReplacementTest("(!!x && y) && z", "x && y && z")
    doReplacementTest("(!!x & y) & z", "x & y & z")
    doReplacementTest("!((((!!x))))", "!x")
  }

  override val inspection: Class<out RInspection>
    get() = BooleanExpressionSimplifier::class.java
}
