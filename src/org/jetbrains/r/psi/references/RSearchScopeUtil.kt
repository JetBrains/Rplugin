/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.r.interpreter.RInterpreterManager.Companion.getInterpreter
import org.jetbrains.r.packages.RPackageProjectManager
import org.jetbrains.r.packages.build.RPackageBuildUtil
import org.jetbrains.r.rmarkdown.RMarkdownFileType

object RSearchScopeUtil {
  fun getScope(element: PsiElement): GlobalSearchScope {
    val project = element.project
    val containingFile = element.containingFile
    return CachedValuesManager.getManager(project).getCachedValue(containingFile) {
      CachedValueProvider.Result.create(createGlobalSearchScope(containingFile, project), PsiModificationTracker.MODIFICATION_COUNT)
    }
  }

  private fun createGlobalSearchScope(containingFile: PsiFile,
                                      project: Project): DelegatingGlobalSearchScope {
    val isTestFile = containingFile.virtualFile?.let { isTestSource(it, project) } ?: false
    RPackageProjectManager.getInstance(project).getProjectPackageDescriptionInfo()
    return object : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
      override fun contains(file: VirtualFile): Boolean {
        if (file == PsiUtilBase.getVirtualFile(containingFile)) {
          return true
        }
        if (file.fileType === RMarkdownFileType) {
          return false
        }
        if (!isTestFile && isTestSource(file, project)) {
          return false
        }
        if (myBaseScope.contains(file)) {
          return true
        }
        val interpreter = getInterpreter(project) ?: return false
        return interpreter.skeletonRoots.contains(file.parent)
      }
    }
  }

  private fun isTestSource(file: VirtualFile, project: Project): Boolean {
    return isUnderRoot(file,getPackageTestRoot(project) ?: return false)
  }

  private fun isUnderRoot(file: VirtualFile, root: VirtualFile): Boolean {
    return VfsUtil.isAncestor(root, file, false)
  }

  private fun getPackageTestRoot(project: Project): VirtualFile? =
    CachedValuesManager.getManager(project).getCachedValue(project) {
      CachedValueProvider.Result.create(
        if (!RPackageBuildUtil.isPackage(project)) null else project.guessProjectDir()?.findChild("tests"),
        VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
      )
    }
}