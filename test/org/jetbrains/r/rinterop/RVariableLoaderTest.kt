/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

class RVariableLoaderTest : RProcessHandlerBaseTestCase() {
  fun testVariables() {
    rInterop.executeCode("""
      aaa <- 123
      bbb <- c("A", "BB", "CCC")
      ccc = function(x, y) x + y
      ddd = list(i = 1, j = 2)
      eee = new.env()
      delayedAssign("fff", 1 + 2)
    """.trimIndent())
    val vars = rInterop.globalEnvLoader.variables.filter { !it.name.startsWith('.') }
    TestCase.assertEquals(listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff"), vars.map { it.name })
    TestCase.assertEquals(listOf(
      RValueSimple::class.java,
      RValueSimple::class.java,
      RValueFunction::class.java,
      RValueList::class.java,
      RValueEnvironment::class.java,
      RValueUnevaluated::class.java
    ), vars.map { it.value.javaClass })
  }

  fun testHiddenAndPartial() {
    rInterop.executeCode("""
      e <- new.env()
      e $ a <- 1
      e $ b <- 2
      e $ c <- 3
      e $ .a <- 4
      e $ .b <- 5
      e $ .c <- 6
    """.trimIndent())
    val loader = RRef.expressionRef("e", rInterop).createVariableLoader()
    TestCase.assertEquals(
      setOf("a" to "[1] 1", "b" to "[1] 2", "c" to "[1] 3", ".a" to "[1] 4", ".b" to "[1] 5", ".c" to "[1] 6"),
      loader.variables.map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
    )
    loader.loadVariablesPartially(0, 10, withHidden = false).blockingGet(DEFAULT_TIMEOUT)!!.let { result ->
      TestCase.assertEquals(
        setOf("a" to "[1] 1", "b" to "[1] 2", "c" to "[1] 3"),
        result.vars.map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
      )
      TestCase.assertEquals(3, result.totalCount)
    }

    val result1 = loader.loadVariablesPartially(0, 2, withHidden = false).blockingGet(DEFAULT_TIMEOUT)!!
    val result2 = loader.loadVariablesPartially(2, 4, withHidden = false).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(3, result1.totalCount)
    TestCase.assertEquals(3, result2.totalCount)
    TestCase.assertEquals(2, result1.vars.size)
    TestCase.assertEquals(1, result2.vars.size)
    TestCase.assertEquals(
      setOf("a" to "[1] 1", "b" to "[1] 2", "c" to "[1] 3"),
      (result1.vars + result2.vars).map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
    )

    val result3 = loader.loadVariablesPartially(0, 2).blockingGet(DEFAULT_TIMEOUT)!!
    val result4 = loader.loadVariablesPartially(2, 6).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(6, result3.totalCount)
    TestCase.assertEquals(6, result4.totalCount)
    TestCase.assertEquals(2, result3.vars.size)
    TestCase.assertEquals(4, result4.vars.size)
    TestCase.assertEquals(
      setOf("a" to "[1] 1", "b" to "[1] 2", "c" to "[1] 3", ".a" to "[1] 4", ".b" to "[1] 5", ".c" to "[1] 6"),
      (result3.vars + result4.vars).map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
    )
  }

  fun testNoFunctions() {
    rInterop.executeCode("""
      e <- new.env()
      e $ v1 <- 1
      e $ v2 <- 2
      e $ v3 <- 3
      e $ f1 <- function(x1) {}
      e $ f2 <- function(x2) {}
      e $ f3 <- function(x3) {}
    """.trimIndent())
    val loader = RRef.expressionRef("e", rInterop).createVariableLoader()
    val set = setOf("v1" to "[1] 1", "v2" to "[1] 2", "v3" to "[1] 3")

    loader.loadVariablesPartially(0, 10, noFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!.let { result ->
      TestCase.assertEquals(
        set, result.vars.map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
      )
      TestCase.assertEquals(3, result.totalCount)
    }

    val result1 = loader.loadVariablesPartially(0, 2, noFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!
    val result2 = loader.loadVariablesPartially(2, 4, noFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(3, result1.totalCount)
    TestCase.assertEquals(3, result2.totalCount)
    TestCase.assertEquals(2, result1.vars.size)
    TestCase.assertEquals(1, result2.vars.size)
    TestCase.assertEquals(
      set,
      (result1.vars + result2.vars).map { it.name to (it.value as RValueSimple).text.trim() }.toSet()
    )
  }

  fun testOnlyFunctions() {
    rInterop.executeCode("""
      e <- new.env()
      e $ v1 <- 1
      e $ v2 <- 2
      e $ v3 <- 3
      e $ f1 <- function(x1) {}
      e $ f2 <- function(x2) {}
      e $ f3 <- function(x3) {}
    """.trimIndent())
    val loader = RRef.expressionRef("e", rInterop).createVariableLoader()
    val set = setOf("f1" to "function(x1)", "f2" to "function(x2)", "f3" to "function(x3)")

    loader.loadVariablesPartially(0, 10, onlyFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!.let { result ->
      TestCase.assertEquals(
        set,
        result.vars.map { it.name to (it.value as RValueFunction).header.trim() }.toSet()
      )
      TestCase.assertEquals(3, result.totalCount)
    }

    val result1 = loader.loadVariablesPartially(0, 2, onlyFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!
    val result2 = loader.loadVariablesPartially(2, 4, onlyFunctions = true).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(3, result1.totalCount)
    TestCase.assertEquals(3, result2.totalCount)
    TestCase.assertEquals(2, result1.vars.size)
    TestCase.assertEquals(1, result2.vars.size)
    TestCase.assertEquals(
      set,
      (result1.vars + result2.vars).map { it.name to (it.value as RValueFunction).header.trim() }.toSet()
    )
  }

  fun testEnvironment() {
    rInterop.executeCode("""
      a = 1
      b = 2
      e = new.env()
      e$ b = 2
      e$ c = 3
    """.trimIndent())
    val vars = rInterop.globalEnvLoader.variables.find { it.name == "e" }!!.ref.createVariableLoader().variables
    TestCase.assertEquals(listOf("b", "c"), vars.map { it.name })
  }

  fun testList() {
    rInterop.executeCode("""
      a = list(11, xx = 22, 33, aa = 44)
    """.trimIndent())
    val vars = rInterop.globalEnvLoader.variables.find { it.name == "a" }!!.ref.createVariableLoader().variables
    TestCase.assertEquals(listOf("", "xx", "", "aa"), vars.map { it.name })
    TestCase.assertEquals(listOf("[1] 11", "[1] 22", "[1] 33", "[1] 44"), vars.map { (it.value as RValueSimple).text })
  }

  fun testListPart() {
    rInterop.executeCode("""
      a = list(aa = 11, bb = 22, cc = 33, dd = 44)
    """.trimIndent())
    val (vars, total) = rInterop.globalEnvLoader.variables.find { it.name == "a" }!!
      .ref.createVariableLoader().loadVariablesPartially(1, 3).blockingGet(DEFAULT_TIMEOUT)!!
    TestCase.assertEquals(4, total)
    TestCase.assertEquals(listOf("bb", "cc"), vars.map { it.name })
    TestCase.assertEquals(listOf("[1] 22", "[1] 33"), vars.map { (it.value as RValueSimple).text })
    TestCase.assertEquals(listOf("[1] 22", "[1] 33"), vars.map { it.ref.evaluateAsTextAsync().get().trim() })
  }

  fun testVector() {
    rInterop.executeCode("a = c(10, 20, 30, 40)")
    val varsA = rInterop.globalEnvLoader.variables.find { it.name == "a" }!!.ref.createVariableLoader().variables
    TestCase.assertEquals(listOf("", "", "", ""), varsA.map { it.name })
    TestCase.assertEquals(listOf("[1] 10", "[1] 20", "[1] 30", "[1] 40"), varsA.map { (it.value as RValueSimple).text })
    rInterop.executeCode("""
      b = c(10, 20, 30, 40)
      names(b) = c("X", "Y", "Z", "W")
    """.trimIndent())
    rInterop.invalidateCaches()
    val varsB = rInterop.globalEnvLoader.variables.find { it.name == "b" }!!.ref.createVariableLoader().variables
    TestCase.assertEquals(listOf("X", "Y", "Z", "W"), varsB.map { it.name })
    TestCase.assertEquals(listOf("[1] 10", "[1] 20", "[1] 30", "[1] 40"), varsB.map { (it.value as RValueSimple).text })
  }

  fun testMatrix() {
    rInterop.executeCode("""
      a <- array(1:20, c(4, 5))
      b <- array(1:48, c(2, 3, 4, 2))
    """.trimIndent())
    TestCase.assertEquals(listOf(4, 5), (RRef.expressionRef("a", rInterop).getValueInfo() as RValueMatrix).dim)
    TestCase.assertEquals(listOf(2, 3, 4, 2), (RRef.expressionRef("b", rInterop).getValueInfo() as RValueMatrix).dim)
  }

  fun testInvalidateCaches() {
    rInterop.executeCode("a = 1")
    TestCase.assertEquals("[1] 1", (rInterop.globalEnvLoader.variables.first { it.name == "a" }.value as RValueSimple).text.trim())
    rInterop.executeCode("a = 555")
    rInterop.invalidateCaches()
    TestCase.assertEquals("[1] 555", (rInterop.globalEnvLoader.variables.first { it.name == "a" }.value as RValueSimple).text.trim())
  }

  fun testFunctionHeader() {
    val header = "function(x, y = 0, z = x + y)"
    val code = """
      $header {
        if (x > y) {
          return(z)
        }
        a <- 0
        for (i in 1:z)
          a <- a + i
        a
      }
    """.trimIndent()
    rInterop.executeCode("ff = $code")
    val loadedHeader = (RRef.expressionRef("ff", rInterop).getValueInfo() as RValueFunction).header
    TestCase.assertTrue(StringUtil.equalsIgnoreWhitespaces(header, loadedHeader))
  }

  fun testFunctionHeaderShow() {
    TestCase.assertTrue(StringUtil.equalsIgnoreWhitespaces(
      "function (object)",
      (RRef.expressionRef("show", rInterop).getValueInfo() as RValueFunction).header
    ))
  }

  fun testAttributes() {
    rInterop.executeCode("""
      xyz <- 55
      attr(xyz, "A1") <- 10
      attr(xyz, "A2") <- 20
    """.trimIndent())
    val attributes = RRef.expressionRef("xyz", rInterop).getAttributesRef()
      .createVariableLoader().variables
      .map { it.name to it.ref }
      .toMap()
    TestCase.assertEquals("[1] 10", attributes["A1"]?.evaluateAsTextAsync()?.get()?.trim())
    TestCase.assertEquals("[1] 20", attributes["A2"]?.evaluateAsTextAsync()?.get()?.trim())
  }
}