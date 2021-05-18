/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.s4class.UnknownS4GenericInspection

class UnknownS4GenericInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testSetMethod() {
    doExprTest("setMethod(${makeError("foo")})")
    doExprTest("setMethod(${makeError("sho")})")
  }

  fun testCorrect() {
    doExprTest("setMethod('show')")
    doExprTest("""
      setGeneric('foo')
      setMethod('foo')
    """.trimIndent())
  }

  override val inspection = UnknownS4GenericInspection::class.java

  companion object {
    private fun msg(functionName: String) = RBundle.message("inspection.unknown.s4.generic.description", functionName)
    private fun makeError(functionName: String): String {
      return "'<error descr=\"${msg(functionName)}\">$functionName</error>'"
    }
  }
}