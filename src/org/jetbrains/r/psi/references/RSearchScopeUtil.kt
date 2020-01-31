/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.psi.references

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.r.interpreter.RInterpreterManager.Companion.getInterpreter
import org.jetbrains.r.rmarkdown.RMarkdownFileType

object RSearchScopeUtil {
  fun getScope(element: PsiElement): GlobalSearchScope {
    return object : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(element.project)) {
      override fun contains(file: VirtualFile): Boolean {
        if (file == PsiUtilBase.getVirtualFile(element)) {
          return true
        }
        if (file.fileType === RMarkdownFileType) {
          return false
        }
        if (myBaseScope.contains(file)) {
          return true
        }
        val interpreter = getInterpreter(element.project) ?: return false
        return interpreter.skeletonRoots.contains(file.parent)
      }
    }
  }
}