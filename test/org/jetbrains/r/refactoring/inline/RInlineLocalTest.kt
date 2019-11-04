// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.refactoring.RInlineAssignmentProcessor

class InlineAssignmentTest : LightPlatformCodeInsightFixtureTestCase() {

  fun testTokenSimple() {
    doTest("inline = 4\ninline + inline", "4 + 4")
  }

  fun testTokenSimple2() {
    doTest("inline = 8\ninline + inline", "8 + 8")
  }

  // todo add more tests here

  private fun doTest(/*@Language("R")*/text: String, /*@Language("R")*/ expected: String) {
    val file = myFixture.configureByText("a.r", text)

    val rule = PsiTreeUtil.getChildOfType(file, RAssignmentStatement::class.java)
    assertNotNull(rule)

    RInlineAssignmentProcessor(rule!!, project, null, false).run()
    assertSameLines(expected, file.text)
  }
}
