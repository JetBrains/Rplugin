package org.jetbrains.r.editor

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import org.jetbrains.plugins.notebooks.visualization.extractTextAndCaretOffset
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigInteger
import kotlin.random.Random

@RunWith(JUnit4::class)
abstract class RMarkdownEditorUiTestBase {
  lateinit var fixture: CodeInsightTestFixture

  @Before
  fun fixtureSetUp() {
    fixture = with(IdeaTestFixtureFactory.getFixtureFactory()) {
      createCodeInsightFixture(
        createFixtureBuilder(RMarkdownEditorUiTestBase::class.java.name, true).fixture,
        TempDirTestFixtureImpl())
    }
    fixture.testDataPath = "${PathManager.getHomePath()}/rplugin/testData/editor"
    fixture.setUp()
  }

  @After
  fun fixtureTearDown() {
    fixture.tearDown()
  }
}

internal fun CodeInsightTestFixture.openNotebookTextInEditor(text: String) {
  val (textWithoutCaret, caretOffset) = extractTextAndCaretOffset(text)

  val localFile = runWriteAction {
    tempDirFixture.createFile("notebook_${BigInteger(1, Random.nextBytes(8)).toString(36)}.rmd")
  }

  val psiFile = PsiManager.getInstance(project).findFile(localFile)!!
  val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!

  runWriteAction {
    document.setText(textWithoutCaret)
    PsiDocumentManager.getInstance(project).commitDocument(document)
    FileDocumentManager.getInstance().saveDocument(document)
  }

  openFileInEditor(localFile)

  caretOffset?.let { editor.caretModel.moveToOffset(it) }
}
