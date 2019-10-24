// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rename

import org.jetbrains.r.RFileType.DOT_R_EXTENSION
import org.jetbrains.r.RLightCodeInsightFixtureTestCase

class RRenameTest : RLightCodeInsightFixtureTestCase() {

  fun testRenameFunction() = doTestWithProject("test_function1")

  fun testRenameFunctionUsage() = doTestWithProject("test_function1")

  fun testRenameParameter() = doTestWithProject("x1")

  fun testRenameParameterUsage() = doTestWithProject("x1")

  fun testRenameLocalVariable() = doTestWithProject("ttt1")

  fun testRenameLocalVariableUsage() = doTestWithProject("ttt1")

  fun testRenameLocalVariableClosure() = doTestWithProject("ttt1")

  fun testRenameLocalVariableClosureUsage() = doTestWithProject("ttt1")

  private fun doTestWithProject(newName: String) {
    myFixture.configureByFile("rename/" + getTestName(true) + DOT_R_EXTENSION)
    myFixture.renameElementAtCaret(newName)
    myFixture.checkResultByFile("rename/" + getTestName(true) + ".after.R", true)
  }
}
