/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.s4class.UnknownS4ClassNameInspection

class UnknownS4ClassNameInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testNew() {
    doExprTest("new(${makeError("numeri")})")
  }

  fun testSlots() {
    doExprTest("setClass('Person', slots = list(age = ${makeError("hello")}))")
  }

  fun testNewClassName() {
    doExprTest("setClass('MyClass')")
  }

  override val inspection = UnknownS4ClassNameInspection::class.java

  companion object {
    private fun msg(className: String) = RBundle.message("inspection.unknown.s4.class.name.description", className)
    private fun makeError(className: String): String {
      return "'<error descr=\"${msg(className)}\">$className</error>'"
    }
  }
}