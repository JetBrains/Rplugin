package org.jetbrains.r.icons

import com.intellij.icons.AllIcons
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import icons.RIcons
import org.jetbrains.r.packages.build.RPackageBuildUtil
import javax.swing.Icon

class RProjectIconProvider : IconProvider() {
  override fun getIcon(element: PsiElement, flags: Int): Icon? {
    if (RPackageBuildUtil.isPackage(element.project)) {
      if (element is PsiFile && isInTestDirectory(element)) {
        return RIcons.Packages.RTest
      }
      if (element is PsiDirectory && isInProjectBasePath(element)) {
        return when (element.name) {
          "tests" -> AllIcons.Modules.TestRoot
          "src" -> AllIcons.Modules.SourceRoot
          "R" -> AllIcons.Modules.SourceRoot
          "man" -> RIcons.Packages.DocumentsRoot
          else -> null
        }
      }
    }
    return null
  }

  private fun isInTestDirectory(e: PsiFile): Boolean {
    val parent = e.parent ?: return false
    return (parent.name == "testthat" && parent.parent != null && parent.parent!!.name == "tests")
           || parent.name == "tests"
  }

  private fun isInProjectBasePath(e: PsiFileSystemItem): Boolean {
    return (e.parent ?: return false).virtualFile.path == e.project.basePath
  }
}