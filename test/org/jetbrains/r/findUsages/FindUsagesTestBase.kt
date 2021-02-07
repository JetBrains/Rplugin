/*
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.findUsages

import com.intellij.testFramework.UsefulTestCase
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTargetUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

abstract class FindUsagesTestBase : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun doTest(@Language("R") code: String, expected: String) {
    myFixture.configureByText("test.R", code.trimIndent())
    val element = myFixture.elementAtCaret
    val targets = UsageTargetUtil.findUsageTargets(element)
    assertNotNull(targets)
    assertTrue(targets.size > 0)
    val target = (targets[0] as PsiElementUsageTarget).element
    val actual = myFixture.getUsageViewTreeTextRepresentation(target)
    UsefulTestCase.assertSameLines(expected.trimIndent(), actual)
  }
}