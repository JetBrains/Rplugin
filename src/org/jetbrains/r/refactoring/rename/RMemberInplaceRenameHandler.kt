/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RPsiElement

class RMemberInplaceRenameHandler : MemberInplaceRenameHandler() {
  override fun doRename(elementToRename: PsiElement, editor: Editor, dataContext: DataContext?): InplaceRefactoring? {
    val realElement =
      if (elementToRename is RIdentifierExpression) elementToRename.parent
      else elementToRename
    return super.doRename(realElement, editor, dataContext)
  }

  override fun createMemberRenamer(element: PsiElement, elementToRename: PsiNameIdentifierOwner, editor: Editor): MemberInplaceRenamer {
    return RMemberInplaceRenamer(elementToRename, elementToRename, editor)
  }

  override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
    if (element !is RPsiElement || RPsiUtil.isLibraryElement(element)) return false
    val realElement =
      if (element is RIdentifierExpression) element.parent
      else element
    return super.isAvailable(realElement, editor, file)
  }
}