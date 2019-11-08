/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.mock.setupMockInterpreterManager
import org.jetbrains.r.psi.references.RReferenceBase
import java.io.File

abstract class RUsefulTestCase : BasePlatformTestCase() {

  private var isLibraryAdded = false

  override fun getTestDataPath() : String {
    return TEST_DATA_PATH
  }

  public override fun setUp() {
    super.setUp()
    myFixture.testDataPath = testDataPath
  }

  public override fun tearDown() {
    isLibraryAdded = false
    super.tearDown()
  }

  fun addLibraries() {
    myFixture.project.setupMockInterpreterManager()
  }

  protected fun doExprTest(expressionList: String): CodeInsightTestFixture {
    myFixture.configureByText("a.R", expressionList)
    configureFixture(myFixture)
    myFixture.testHighlighting(true, false, false)

    return myFixture
  }

  fun doApplyCompletionTest(text: String, elementName: String, expected: String, fileIsRConsole: Boolean = false, fileExtension: String = "R") {
    myFixture.configureByText("foo.$fileExtension", text)
    if (fileIsRConsole) {
      myFixture.file.putUserData(RConsoleView.IS_R_CONSOLE_KEY, true)
    }
    val result = myFixture.completeBasic()
    TestCase.assertNotNull(result)
    val element = result.first { it.lookupString == elementName }
    myFixture.lookup.currentItem = element
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    val caretPosition = myFixture.caretOffset
    val newText = myFixture.file.text
    TestCase.assertEquals(expected, "${newText.substring(0, caretPosition)}<caret>${newText.substring(caretPosition, newText.length)}")
  }


  protected open fun configureFixture(myFixture: CodeInsightTestFixture) {}

  private fun createFixture(): CodeInsightTestFixture {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val fixtureBuilder = factory.createLightFixtureBuilder()
    val fixture = fixtureBuilder.fixture
    return IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, LightTempDirTestFixtureImpl(true))
  }

  protected fun createAnActionEvent(): AnActionEvent =
    AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_POPUP, null, createDataContext())

  protected open fun createDataContext(): DataContext {
    return DataContext {
      when (it) {
        CommonDataKeys.PROJECT.name -> myFixture.project
        CommonDataKeys.EDITOR.name -> myFixture.editor
        CommonDataKeys.PSI_FILE.name -> myFixture.file
        CommonDataKeys.VIRTUAL_FILE.name -> myFixture.file.virtualFile
        else -> null
      }
    }
  }

  protected fun resolve(): Array<ResolveResult> {
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val rReferenceBase = reference as RReferenceBase<*>
    return rReferenceBase.multiResolve(false)
  }

  protected fun <T : PsiElement> findElementAtCaret(aClass: Class<T>): T? {
    return PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), aClass, false)
  }

  companion object {
    private val TEST_DATA_PATH = File("testData").absolutePath.replace(File.pathSeparatorChar, '/')
    val SKELETON_LIBRARY_PATH = TEST_DATA_PATH + "/skeletons"
  }
}

