/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown.structureView

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.r.psi.RGlobalVariablesFilter
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil

class RMarkdownStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
    return object : TreeBasedStructureViewBuilder() {
      override fun createStructureViewModel(editor: Editor?): StructureViewModel {
        return RMarkdownStructureViewModel(psiFile, editor)
      }

      override fun isRootNodeShown(): Boolean {
        return false
      }
    }
  }

  private class RMarkdownStructureViewModel internal constructor(psiFile: PsiFile, editor: Editor?)
    : StructureViewModelBase(psiFile, editor, RMarkdownStructureElement(psiFile)) {

    override fun getFilters(): Array<Filter> = arrayOf<Filter>(RGlobalVariablesFilter())

    override fun findAcceptableElement(e: PsiElement?): Any? {
      var element = e
      // walk up the psi-tree until we find an element from the structure view
      while (element != null && !RMarkdownPsiUtil.PRESENTABLE_TYPES.contains(PsiUtilCore.getElementType(element))) {
        val parentType = PsiUtilCore.getElementType(element.parent)

        val previous = element.prevSibling
        if (previous == null || !RMarkdownPsiUtil.TRANSPARENT_CONTAINERS.contains(parentType)) {
          element = element.parent
        }
        else {
          element = previous
        }
      }

      return element
    }
  }
}
