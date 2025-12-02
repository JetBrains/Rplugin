/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.annotator

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import com.intellij.r.psi.highlighting.RColorSettingsPage
import java.io.File
import java.nio.file.Paths

class RAnnotatorTest : RLightCodeInsightFixtureTestCase() {

  override fun getTestDataPath(): String = Paths.get(super.getTestDataPath(), "annotator").toString()

  fun testLocalVariableHighlighting() = doTest()

  fun testFunctionDeclaration() = doTest()

  fun testFunctionCall() = doTest()

  fun testFor() = doTest()

  fun testClosure() = doTest()

  fun testNamedParameters() = doTest()

  fun testAtOperator() = doTest()

  fun testListSubsetOperator() = doTest()

  fun testDots() = doTest()

  fun testHighlightParameterDefaultValue() = doTest()

  fun testArgumentsWithoutComma() = doTest()

  fun testLambdaCall() = doTest()

  fun testListUnclosedString() = doTest()

  fun testSourceAndLinks() {
    addLibraries()

    val shortcuts = KeymapManager.getInstance().activeKeymap.getShortcuts(IdeActions.ACTION_GOTO_DECLARATION)
    val shortcutText = buildString {
      ContainerUtil.find(shortcuts) { !it.isKeyboard }?.let {
        append(KeymapUtil.getShortcutText(it).replace(Regex("Button\\d "), ""))
      }

      ContainerUtil.find(shortcuts) { it.isKeyboard }?.let {
        if (isNotEmpty()) append(", ")
        append(KeymapUtil.getShortcutText(it))
      }
    }

    val fileText = File(testDataPath, getTestName(true) + ".R")
      .readText()
      .replace("ACTION_GOTO_DECLARATION", shortcutText)

    myFixture.configureByText("a.R", fileText)
    myFixture.checkHighlighting(false, true, false)
  }

  fun testSampleFromColorSettingsPage() {
    myFixture.configureByText("foo.R", RColorSettingsPage.R_DEMO_FOR_TESTS)
    myFixture.checkHighlighting(false, true, false)
  }

  private fun doTest() {
    val testName = getTestName(true)
    myFixture.testHighlighting(true, true, false, "$testName.R")
  }

}