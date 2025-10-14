/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import junit.framework.TestCase
import com.intellij.r.psi.RLanguage
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import com.intellij.r.psi.psi.api.RIdentifierExpression

class RCrossFileResolve: RLightCodeInsightFixtureTestCase() {

  fun testResolveInTheSameFile() {
    myFixture.configureByFiles("resolve/foo1.R", "resolve/foo2.R")
    val rIdentifierExpression = myFixture.findElementByText("baz.bar() # here", RIdentifierExpression::class.java)
    val multiResolve = rIdentifierExpression.reference.multiResolve(false)
    TestCase.assertTrue(multiResolve.size == 1)
    TestCase.assertEquals(myFixture.file, multiResolve[0].element?.containingFile)
  }

  fun testResolveInAnotherFile() {
    val files = myFixture.configureByFiles("resolve/foo2.R", "resolve/foo1.R")
    val rIdentifierExpression = myFixture.findElementByText("foo.bar() # here", RIdentifierExpression::class.java)
    val multiResolve = rIdentifierExpression.reference.multiResolve(false)
    TestCase.assertTrue(multiResolve.size == 1)
    TestCase.assertEquals(files[1], multiResolve[0].element?.containingFile)
  }

  fun testResolveRMarkdownInTheSameFile() {
    myFixture.configureByFiles("resolve/foo2.Rmd", "resolve/foo1.R")
    val rIdentifierExpression = myFixture.findElementByText("foo.bar() # here", RIdentifierExpression::class.java)
    val multiResolve = rIdentifierExpression.reference.multiResolve(false)
    TestCase.assertTrue(multiResolve.size == 1)
    TestCase.assertEquals(myFixture.file.viewProvider.getPsi(RLanguage.INSTANCE), multiResolve[0].element?.containingFile)
  }

  fun testDontResolveToRMarkdown() {
    myFixture.configureByFiles("resolve/foo2.R", "resolve/foo2.Rmd")
    val rIdentifierExpression = myFixture.findElementByText("foo.bar() # here", RIdentifierExpression::class.java)
    val multiResolve = rIdentifierExpression.reference.multiResolve(false)
    TestCase.assertTrue(multiResolve.isEmpty())
  }

  fun testResolveFromRMarkdown() {
    val files = myFixture.configureByFiles("resolve/foo1.Rmd", "resolve/foo1.R")
    val rIdentifierExpression = myFixture.findElementByText("baz.bar() # here", RIdentifierExpression::class.java)
    val multiResolve = rIdentifierExpression.reference.multiResolve(false)
    TestCase.assertTrue(multiResolve.size == 1)
    TestCase.assertEquals(files[1], multiResolve[0].element?.containingFile)
  }
}

val x = 1231
