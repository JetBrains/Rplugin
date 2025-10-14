/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rename

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.NameSuggestionProvider
import com.intellij.refactoring.rename.PreferrableNameSuggestionProvider
import com.intellij.r.psi.RFileType.DOT_R_EXTENSION
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RParameter

class RNameSuggestionProviderTest : RLightCodeInsightFixtureTestCase() {

  fun testVariable() = doTest()

  fun testVariableUsage() = doTest()

  fun testFunction() = doTest()

  fun testFunctionUsage() = doTest()

  fun testForLoop() = doTest()

  fun testForLoopUsage() = doTest()

  fun testVariableWithDots() = doTest()

  fun testFunctionWithDots() = doTest()

  fun testForLoopWithDots() = doTest()

  fun testVariableCollisions() = doTest()

  fun testFunctionCollisions() = doTest()

  fun testForLoopCollisions1() = doTest()

  fun testForLoopCollisions2() = doTest()

  fun testForLoopCollisions3() = doTest()

  fun testParameter() = doTest()

  private fun doTest() {
    myFixture.configureByFile("name_suggestion/" + getTestName(true) + DOT_R_EXTENSION)

    val element = TargetElementUtil.findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted())
    assertNotNull(element)

    val realElement = if (element is RIdentifierExpression) {
      RPsiUtil.getAssignmentByAssignee(element) ?: if (element.parent is RParameter) element.parent else element
    }
    else element

    val names = mutableSetOf<String>()
    val expectedNames = getExpectedNames()
    suggestNames(realElement!!, names)
    assertEquals(expectedNames, names)
  }

  private fun getExpectedNames(): Set<String> {
    val message = "Suggested names no find in file ${myFixture.file.name}"
    val comment = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiComment::class.java).lastOrNull() ?: error(message)
    val text = comment.text.trim()
    if (!text.startsWith(PREFIX)) error(message)
    val variants = text.removePrefix(PREFIX).split(",").map { it.trim() }
    if (variants.isEmpty()) error("Specify at least 1 variant")
    return variants.toSet()
  }

  private fun suggestNames(psiElement: PsiElement, result: MutableSet<String>) {
    for (provider in NameSuggestionProvider.EP_NAME.extensionList) {
      val info = provider.getSuggestedNames(psiElement, null, result)
      if (info != null) {
        if (provider is PreferrableNameSuggestionProvider && !provider.shouldCheckOthers()) break
      }
    }
  }

  companion object {
    const val PREFIX = "# SUGGESTED: "
  }
}
