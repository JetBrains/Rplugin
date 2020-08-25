package org.jetbrains.r.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase
import java.io.IOException

class CreateRTestFileActionTest : RUsefulTestCase() {

  override fun setUp() {
    super.setUp()
    val projectView = ProjectView.getInstance(project) as ProjectViewImpl
    val pane = ProjectViewPane(project)
    projectView.addProjectPane(pane)
  }

  fun testNew() {
    doTest("new")
  }

  fun testNewTestthatExists() {
    doTest("NewTestthatExists", testThatExist = true)
    myFixture.configureByFile("tests/testthat.R")
    TestCase.assertEquals("test", myFixture.file.text)
  }

  fun testExistTestthatNot() {
    doTest("ExistTestthatNot", currentTestExist = true)
    TestCase.assertEquals("test", myFixture.configureByFile("tests/testthat/test-ExistTestthatNot.R").text)
  }

  fun testAndTestthatExist() {
    doTest("AndTestthatExist", currentTestExist = true, testThatExist = true)
    TestCase.assertEquals("test", myFixture.configureByFile("tests/testthat.R").text)
    TestCase.assertEquals("test", myFixture.configureByFile("tests/testthat/test-AndTestthatExist.R").text)
  }

  private fun doTest(
    testFileName: String,
    testThatExist: Boolean = false,
    currentTestExist: Boolean = false,
    configureBy: String = "R/$testFileName.R"
  ) {
    val project = project
    try {
      val tempDir = myFixture.tempDirFixture.getFile(".")
      val directory = PsiManager.getInstance(project).findDirectory(tempDir!!)
      myFixture.addFileToProject("DESCRIPTION", "")
      myFixture.addFileToProject("R/$testFileName.R", "")
      if (testThatExist) {
        myFixture.addFileToProject("tests/testthat.R", "test")
        myFixture.tempDirFixture.findOrCreateDir("tests/testthat")
      }
      if (currentTestExist) {
        myFixture.addFileToProject("tests/testthat/test-$testFileName.R", "test")
      }
      myFixture.configureByFile(configureBy)
      val myActionEvent = createAnActionEvent(listOf(directory!!))
      val action = CreateRTestFileAction()
      action.actionPerformed(myActionEvent)
      for (path in listOf("tests/testthat/test-$testFileName.R", "tests/testthat.R")) {
        TestCase.assertNotNull(myFixture.tempDirFixture.getFile(path))
      }
    }
    catch (e: IOException) {
      throw RuntimeException(e)
    }
  }
}