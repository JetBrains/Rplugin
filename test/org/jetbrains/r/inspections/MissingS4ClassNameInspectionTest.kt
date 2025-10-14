/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.r.psi.RBundle
import org.jetbrains.r.inspections.classes.s4.MissingS4ClassNameInspection

class MissingS4ClassNameInspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testSlots() {
    doExprTest("setClass('MyClass', slots = list(name = 'character', age = $errorStr))")
    doExprTest("setClass('MyClass', slots = c(field = c('character', age = $errorStr)))")
  }

  fun testContains() {
    doExprTest("setClass('MyClass', contains = $errorStr)")
    doExprTest("setClass('MyClass', contains = 'numeric')")
  }

  fun testSetMethodSignature() {
    doExprTest("setMethod('foo', c($errorStr, 'numeric'))")
  }

  override val inspection = MissingS4ClassNameInspection::class.java

  companion object {
    private val errorStr = "<error descr=\"${RBundle.message("inspection.missing.s4.class.name.description")}\">''</error>"
  }
}