// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi

import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.ide.util.treeView.smartTree.Grouper
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.r.psi.psi.api.RAssignmentStatement

class RScriptStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
    return object : TreeBasedStructureViewBuilder() {

      override fun createStructureViewModel(editor: Editor?): StructureViewModel {
        return RStructureViewModel(psiFile)
      }

      override fun isRootNodeShown(): Boolean {
        return false
      }
    }
  }
}

private class RStructureViewModel(private val file: PsiFile) : TextEditorBasedStructureViewModel(file) {
  private val groupers = arrayOf<Grouper>()

  override fun getRoot(): StructureViewTreeElement {
    return RStructureViewElement(file)
  }

  override fun getGroupers(): Array<Grouper> = arrayOf()

  override fun getSorters(): Array<Sorter> {
    // TODO - Enable sorting based on defs, macs, fns, []s, etc...
    return arrayOf(Sorter.ALPHA_SORTER)
  }

  override fun getFilters(): Array<Filter> = arrayOf(RGlobalVariablesFilter())

  override fun getPsiFile(): PsiFile {
    return file
  }

  override fun getSuitableClasses(): Array<Class<*>> {
    return arrayOf(RAssignmentStatement::class.java, PsiComment::class.java)
  }
}
