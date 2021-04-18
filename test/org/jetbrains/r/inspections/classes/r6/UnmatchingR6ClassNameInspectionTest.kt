/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.classes.r6

import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.RInspectionTest

class UnmatchingR6ClassNameInspectionTest : RInspectionTest() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testClassNameInspection() {
    doExprTest("""
      UserClass <- R6Class("UserClass")
    """.trimIndent())

    doExprTest("""
      UserClass <- R6Class(${makeError("MyClass")})
    """.trimIndent())
  }

  override val inspection = UnmatchingR6ClassNameInspection::class.java

  companion object {
    private fun msg(argumentName: String) = RBundle.message("inspection.r6class.naming.convention.classname", argumentName)

    private fun makeError(className: String): String {
      return "<error descr=\"${msg(className)}\">'$className'</error>"
    }
  }
}