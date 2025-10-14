package org.jetbrains.r.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.r.psi.RLanguage
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import com.intellij.r.psi.psi.api.RExpression
import java.io.File
import java.nio.file.Path

abstract class RResolveFromFilesTestCase(testDataDirectoryRoot: String) : RLightCodeInsightFixtureTestCase() {
  protected val fullTestDataPath = "$TEST_DATA_PATH/$testDataDirectoryRoot"

  protected fun getActualResults(): Set<PsiElementWrapper> {
    return resolve().mapNotNull { PsiElementWrapper(it.element ?: return@mapNotNull null) }.toSet()
  }

  protected fun getExpectedResult(keyComment: String, files: List<PsiFile> = getFiles()): Set<PsiElementWrapper> {
    return collectExpectedResult(keyComment, files).map { PsiElementWrapper(it) }.toSet()
  }

  protected open fun getFiles(
    editorFileName: String = "main",
    filterPredicate: (String) -> Boolean = { true }
  ): List<PsiFile> {
    val filepaths = collectFilePaths(editorFileName, filterPredicate)
    val rootPath = Path.of(fullTestDataPath, getTestName(true))
    val virtualFiles= filepaths.map {
      val relativePath = rootPath.relativize(Path.of(it)).toString()
      myFixture.copyFileToProject(it, relativePath)
    }
    myFixture.configureFromExistingVirtualFile(virtualFiles[0])
    return virtualFiles.map { PsiUtilCore.findFileSystemItem(project, it) as PsiFile }
  }

  private fun collectFilePaths(editorFileName: String, filterPredicate: (String) -> Boolean): List<String> {
    val files = File("$fullTestDataPath/" + getTestName(true))
      .walkTopDown()
      .filter { it.isFile }
      .filter { filterPredicate(it.name) }
      .map { it.path }
      .sortedByDescending { it.contains(editorFileName) }
      .toList()
    if (files.isEmpty()) error("No files at $fullTestDataPath")
    return files
  }

  protected class PsiElementWrapper(val elem: PsiElement) {
    override fun equals(other: Any?): Boolean {
      if (other !is PsiElementWrapper) return false
      return elem == other.elem
    }

    override fun hashCode(): Int {
      return elem.hashCode()
    }

    override fun toString(): String {
      return "${(elem as? PsiNamedElement)?.name ?: elem.text} from `${elem.containingFile.name}`"
    }
  }

  private fun collectExpectedResult(keyComment: String, files: List<PsiFile>): List<PsiElement> {
    return files.flatMap { file ->
      val text = file.viewProvider.getPsi(RLanguage.INSTANCE)?.text ?: return@flatMap emptyList<PsiElement>()
      Regex(keyComment).findAll(text).map { it.range.first }.mapNotNull {
        PsiTreeUtil.getPrevSiblingOfType(file.findElementAt(it), RExpression::class.java)
      }.toList()
    }
  }
}