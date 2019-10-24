// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class UnusedVariableInspectionTest : org.jetbrains.r.inspections.RInspectionTest() {

  fun testUnusedVariable() {
    doExprTest("a = 3")
  }

  fun testUnusedVariableInFunExpr() {
    doExprTest(readTestDataFile())
  }

  fun testUnusedAnonymousFunExpr() {
    doExprTest("function(x)x")
  }

  fun testUnusedFunction() {
    doExprTest("myfun = function(x)x")
  }

  fun testColumnDeleteByNullAssign() {
    doExprTest("foo = data.frame() ; foo\$bar <- NULL; head(foo)")
  }

  fun testFlagUnusedMemberAccess() {
    doExprTest("foo = data.frame() ; " + "foo\$bar" + " <- 3")
  }

  fun testOutsideBlockUsage() {
    doExprTest("{ a = 3; }; a")
  }

  fun testArrayModification() {
    doExprTest("a = 1:5; a[4] = 3; 1 + a")
  }

  fun testDedicatedAccessorFunction() {
    doExprTest("a = data.frame(col1=1:3, col2=NA); rownames(a) = c('foo', 'bar');  a")
  }

  fun testUnusedAssignment() {
    doExprTest("""
      function(x, y) {
        print(x)
        ${warnReassigned("x")} <- 10
        x <- 20
      }
    """.trimIndent())
  }

  fun testUsageOutsideIfElse() {
    doExprTest("if(T){ a = 3; }else{ b = 2; }; a ; b")
  }

  fun testDonFlagReturn() {
    assertAllUsed("function(){ if(T){ head(iris); return(1) };  return(2); { return(3) }; return(4) }()")
  }

  fun testDontFlagLastFunExprStatement() {
    assertAllUsed("myfun = function(){ a = 3 }; myfun()")
  }

  fun testDontFlagLastBlockExprStatement() {
    assertAllUsed("{ foo= 3; foo }")
  }

  fun testFlagLastBlockIfNotAssignedOrReturn() {
    assertAllUsed("myfun = function(){ head(iris); { a = 3} }; myfun()")
  }

  fun testDontFlagExprInTerminalIfElse() { // this already not really realistic, but for sake of completeness
    assertAllUsed("myfun = function(){ head(iris); if(T){ a = 3} }; myfun()")
  }

  fun testDontFlagFunctionArgUsedAsUnnamedArg() {
    assertAllUsed("function(usedArg) head(usedArg)")
  }

  fun testDontFlagFunctionArgUsedAsNamedArg() {
    assertAllUsed("function(usedArg) head(x=usedArg)")
  }

  fun testQuoteAgnosticOperatorsDefs() {
    assertAllUsed(
      "`%foo%` <- function(a,b) 3\n" +
      "'%bar%' <- function(a,b) 3;\n" +
      "1 %foo% 3;  2 %bar% 3")
  }

  fun testReassignParameter() {
    assertAllUsed(
      """
        function(x) {
          x <- x + 1
          print(x)
          x <- 3545435
          print(x)
        }
      """.trimIndent()
    )
  }

  fun testUsedInAssignedValue() {
    assertAllUsed(
      """
        function(good.names=c()) {
          good.names <- all.names[good.names.idx]
          good.names <- foo(good.names)
        }
      """.trimIndent()
    )
  }

  fun testAssignmentInArguments() {
    assertAllUsed(
      """
        function(x) {
          x <- f(x)
          x <- g(y = x)
        }
      """.trimIndent()
    )
  }

  fun testUsedInFor() {
    assertAllUsed(
      """
        function(x) {
          x <- f(x)
          for (y in x) {
          }
        }
      """.trimIndent()
    )
  }

  fun testIgnoreUnusedClosureAssignment() {
    assertAllUsed(
      """
        xxx <- 312312
        print(xxx)
        zzz <- function() {
          print(xxx)
          xxx <<- 3125435435
        }
        print(xxx)
      """.trimIndent()
    )
  }

  override val inspection: Class<out RInspection>
    get() = UnusedVariableInspection::class.java

  private fun warnUnused(varName: String): String {
    return "<warning descr=\"Variable '$varName' is never used\">$varName</warning>"
  }

  private fun warnReassigned(varName: String): String {
    return "<warning descr=\"Variable '$varName' is assigned but never accessed\">$varName</warning>"
  }
}
