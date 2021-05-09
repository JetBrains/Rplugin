/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.r.classes.s4.RS4ClassPomTarget
import org.jetbrains.r.classes.s4.context.RS4ContextProvider
import org.jetbrains.r.classes.s4.context.setClass.RS4SetClassClassNameContext
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RForStatement
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RPsiElement
import org.jetbrains.r.psi.api.RStringLiteralExpression

class RVariableInplaceRenameHandler : VariableInplaceRenameHandler() {

  override fun createRenamer(elementToRename: PsiElement, editor: Editor): VariableInplaceRenamer {
    return RVariableInplaceRenamer(elementToRename as PsiNamedElement, editor)
  }

  override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
    if (element !is RPsiElement || RPsiUtil.isLibraryElement(element)) return false
    val parent = element.parent
    return when {
      element is RIdentifierExpression && parent is RForStatement && parent.target == element -> true
      element is PomTargetPsiElement && element.target is RS4ClassPomTarget -> true
      element is RStringLiteralExpression && RS4ContextProvider.getS4Context(element, RS4SetClassClassNameContext::class) != null -> true
      else -> super.isAvailable(element, editor, file)
    }
  }
}