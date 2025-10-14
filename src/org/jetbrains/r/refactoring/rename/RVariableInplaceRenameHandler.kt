/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.r.psi.classes.s4.classInfo.RStringLiteralPomTarget
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.classes.s4.context.setClass.RS4SetClassClassNameContext
import com.intellij.r.psi.classes.s4.context.setClass.RS4SlotDeclarationContext
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.api.RForStatement
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RPsiElement
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer

class RVariableInplaceRenameHandler : VariableInplaceRenameHandler() {

  override fun createRenamer(elementToRename: PsiElement, editor: Editor): VariableInplaceRenamer {
    return RVariableInplaceRenamer(elementToRename as PsiNamedElement, editor)
  }

  override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
    if (element !is RPsiElement || RPsiUtil.isLibraryElement(element)) return false
    val parent = element.parent
    return when {
      element is RIdentifierExpression && parent is RForStatement && parent.target == element -> true
      element is PomTargetPsiElement && element.target is RStringLiteralPomTarget -> true
      element is RStringLiteralExpression && RS4ContextProvider.getS4Context(element,
                                                                             RS4SetClassClassNameContext::class,
                                                                             RS4SlotDeclarationContext::class) != null -> true
      else -> super.isAvailable(element, editor, file)
    }
  }
}