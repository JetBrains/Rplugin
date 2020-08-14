/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.s4class.DeprecatedSetClassArgsInspection

class DeprecatedSetClassArgsInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testRepresentation() {
    doExprTest("""
      setClass('MyClass', ${makeWeakWarn("representation", "representation = representation(aaa = 'aaaType')")})
    """.trimIndent(), true)
  }

  fun testRepresentationImplicit() {
    doExprTest("""
      setClass('MyClass', ${makeWeakWarn("representation", "list(aaa = 'aaaType')")})
    """.trimIndent(), true)
  }

  fun testVersion() {
    doExprTest("""
      setClass('MyClass', slots = c(aa = "aaType", bb = "bbType"), ${makeWeakWarn("version", "version = '2.3.5'")})
    """.trimIndent(), true)
  }

  fun testAccess() {
    doExprTest("""
      setClass('MyClass', contains = c("matrix", "VIRTUAL"), ${makeWeakWarn("access", "access = 'ALL'")})
    """.trimIndent(), true)
  }

  fun testS3methods() {
    doExprTest("""
      setClass('MyClass', ${makeWeakWarn("S3methods", "S3methods = TRUE")})
    """.trimIndent(), true)
  }

  fun testNoWarnings() {
    doExprTest("""
      setClass('MyClass', slots = c(aa = "aaType", bb = "bbType"), contains = c("matrix", "VIRTUAL"))
    """.trimIndent(), true)
  }

  override val inspection = DeprecatedSetClassArgsInspection::class.java

  companion object {
    private fun msg(argumentName: String) = RBundle.message("inspection.deprecated.setClass.args.description", argumentName)

    private fun makeWeakWarn(argumentName:String, text: String): String {
      return "<weak_warning descr=\"${msg(argumentName)}\">$text</weak_warning>"
    }
  }
}