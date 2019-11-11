// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import org.jetbrains.r.RFileType.DOT_R_EXTENSION

class MissingPackageInspectionTest : org.jetbrains.r.inspections.RInspectionTest() {

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testMissingFoobarPackage() {
    doTest(getTestName(false) + DOT_R_EXTENSION)
  }


  fun testQuotedPackageName() {
    doTest(getTestName(false) + DOT_R_EXTENSION)
  }


  fun testFunCallRequireArg() {
    doExprTest("require(getPckgName('foo'))")
  }


  fun testMissingPckgInNamespaceCall() {
    doExprTest("<error descr=\"'foobar' is not yet installed\">foobar</error>::myFun()")
  }


  fun testMissingPckgInNonExportedNamespaceCall() {
    doExprTest("<error descr=\"'foobar' is not yet installed\">foobar</error>:::myFun()")
  }

  override val inspection: Class<out RInspection>
    get() = MissingPackageInspection::class.java
}
