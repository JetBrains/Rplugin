/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.templateLanguages.TemplateLanguageUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer

class RMemberInplaceRenamer : MemberInplaceRenamer {
  constructor(elementToRename: PsiNamedElement,
              substituted: PsiElement?,
              editor: Editor) : super(elementToRename, substituted, editor, elementToRename.name, elementToRename.name)

  constructor(elementToRename: PsiNamedElement,
              substituted: PsiElement?,
              editor: Editor,
              initialName: String?,
              oldName: String?) : super(elementToRename, substituted, editor, initialName, oldName)

  override fun acceptReference(reference: PsiReference): Boolean {
    return RenameUtil.acceptReference(reference, myElementToRename)
  }

  override fun createInplaceRenamerToRestart(variable: PsiNamedElement, editor: Editor, initialName: String): VariableInplaceRenamer {
    return RMemberInplaceRenamer(variable, substituted, editor, initialName, myOldName)
  }

  override fun startsOnTheSameElement(handler: RefactoringActionHandler?, element: PsiElement?): Boolean {
    val variable = variable
    return (element == variable || element?.parent == variable) && handler is MemberInplaceRenameHandler
  }

  override fun getRangeToRename(reference: PsiReference): TextRange {
    return RenameUtil.fixTextRange(super.getRangeToRename(reference), reference)
  }

  override fun getRangeToRename(element: PsiElement): TextRange {
    return RenameUtil.fixTextRange(super.getRangeToRename(element), element)
  }

  override fun notSameFile(file: VirtualFile?, containingFile: PsiFile): Boolean {
    val currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.document) ?: return true
    return TemplateLanguageUtil.getBaseFile(containingFile) !== TemplateLanguageUtil.getBaseFile(currentFile)
  }

  override fun getNameIdentifier(): PsiElement? {
    return (myElementToRename as? PsiNameIdentifierOwner)?.nameIdentifier
  }
}