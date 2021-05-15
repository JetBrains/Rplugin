/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.r.classes.s4.classInfo.RStringLiteralPomTarget
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RVariableInplaceRenamer : VariableInplaceRenamer {

  constructor(elementToRename: PsiNamedElement, editor: Editor) : super(elementToRename, editor, elementToRename.project)

  constructor(elementToRename: PsiNamedElement?,
              editor: Editor,
              project: Project,
              initialName: String?,
              oldName: String?) : super(elementToRename, editor, project, initialName, oldName)

  override fun createInplaceRenamerToRestart(variable: PsiNamedElement, editor: Editor, initialName: String): VariableInplaceRenamer {
    return RVariableInplaceRenamer(variable, editor, myProject, initialName, myOldName)
  }

  override fun getVariable(): PsiNamedElement? {
    if (myRenameOffset != null) {
      val file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument()) ?: return null
      return PsiTreeUtil.findElementOfClassAtRange(file, myRenameOffset.getStartOffset(),
                                                   myRenameOffset.getEndOffset(),
                                                   RIdentifierExpression::class.java)
    }
    return super.getVariable()
  }

  override fun getNameIdentifier(): PsiElement? {
    val element = myElementToRename
    if (element is RStringLiteralExpression) return element
    if (element is PomTargetPsiElement) {
      (element.target as? RStringLiteralPomTarget)?.let { return it.literal }
    }
    return super.getNameIdentifier()
  }

  override fun acceptReference(reference: PsiReference): Boolean {
    return RenameUtil.acceptReference(reference, myElementToRename)
  }

  override fun getRangeToRename(reference: PsiReference): TextRange {
    return RenameUtil.fixTextRange(super.getRangeToRename(reference), reference)
  }

  override fun getRangeToRename(element: PsiElement): TextRange {
    if (element is RStringLiteralExpression) {
      return ElementManipulators.getValueTextRange(element)
    }
    return RenameUtil.fixTextRange(super.getRangeToRename(element), element)
  }

  override fun checkLocalScope(): PsiElement? {
    val currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument())
    return currentFile ?: super.checkLocalScope()
  }
}