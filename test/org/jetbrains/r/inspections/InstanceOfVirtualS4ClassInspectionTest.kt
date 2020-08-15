/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.s4class.InstanceOfVirtualS4ClassInspection

class InstanceOfVirtualS4ClassInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testBaseLibraryClass() {
    doTest("glm", true)
  }

  fun testNotVirtualBaseLibrary() {
    doTest("matrix", false)
  }

  fun testDplyr() {
    doTest("tbl", true)
    doTest("rowwise_df", true)
    doTest("rowwise_df", true)
  }

  fun testUser() {
    doExprTest("""
      setClass('MyClass', slots = c('aa', 'bb'))
      new('MyClass')
    """.trimIndent())

    doExprTest("""
      setClass('MyClass', slots = c('aa', 'bb'), contains = "VIRTUAL")
      new(${makeError("MyClass")})
    """.trimIndent())
  }

  override val inspection = InstanceOfVirtualS4ClassInspection::class.java

  private fun doTest(className: String, isVirtual: Boolean) {
    val text = if (isVirtual) makeError(className) else "'$className'"
    doExprTest("new($text)")
  }

  companion object {
    private fun msg(argumentName: String) = RBundle.message("inspection.virtual.s4class.instance.description", argumentName)

    private fun makeError(className: String): String {
      return "<error descr=\"${msg(className)}\">'$className'</error>"
    }
  }
}