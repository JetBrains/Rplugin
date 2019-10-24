// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class UnusedParameterInspectionTest : RInspectionTest() {

  override val inspection: Class<out RInspection>
    get() = UnusedParameterInspection::class.java

  // False negative tests: unused annotation should be present

  fun testUnusedParameterInspection() {
    //    assertUnused(readTestDataFile());
  }

  // False positive tests: Unused annotation might be present (or was by regression) but should not

  /**
   * We should not flag symbols as unused if they are used for named arguments
   */
  fun testDontFlagNamedFunctionParameter() {
    // if this happens, it rather means that the reference resolver is broken (again)
    assertAllUsed("myFun = function(arg) head(x=arg)")
  }

  fun testDontFlagExternallyDefinedArgs() {
    // this test is especially important since the resolver order and the detection of locality matter here
    assertAllUsed("trainData <- iris; function(trainData){   trainData }")
  }

  fun testDontFlagTripleArg() {
    // this test is especially important since the resolver order and the detection of locality matter here
    assertAllUsed("function(...) as.character('foo', ...)")
  }
}
