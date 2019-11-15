// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.console.RConsoleBaseTestCase

class UnresolvedReferenceInspectionConsoleTest : RConsoleBaseTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(UnresolvedReferenceInspection::class.java)
    addLibraries()
  }

  fun testFilter() {
    doTest("filter()")
  }

  fun testNasa() {
    val message = UnresolvedReferenceInspection.missingPackageMessage("nasa", listOf("dplyr"))
    doTest("View(<weak_warning descr=\"$message\">nasa</weak_warning>)")
  }

  fun testNamespaceNasa() {
    doTest("View(dplyr::nasa)")
  }

  fun testGroupBy() {
    val message = UnresolvedReferenceInspection.missingPackageMessage("group_by", listOf("dplyr"))
    doTest("<weak_warning descr=\"$message\">group_by</weak_warning>()")
  }

  fun testNamespaceGroupBy() {
    doTest("dplyr::group_by()")
  }

  private fun doTest(text: String) {
    doExprTest(text, true)
  }
}
