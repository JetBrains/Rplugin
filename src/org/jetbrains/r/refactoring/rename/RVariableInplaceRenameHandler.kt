/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RForStatement
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RPsiElement

class RVariableInplaceRenameHandler : VariableInplaceRenameHandler() {

  override fun createRenamer(elementToRename: PsiElement, editor: Editor): VariableInplaceRenamer? {
    return RVariableInplaceRenamer(elementToRename as PsiNamedElement, editor)
  }

  override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
    if (element !is RPsiElement || RPsiUtil.isLibraryElement(element)) return false
    val parent = element.parent
    if (element is RIdentifierExpression && parent is RForStatement && parent.target == element) return true
    return super.isAvailable(element, editor, file)
  }
}