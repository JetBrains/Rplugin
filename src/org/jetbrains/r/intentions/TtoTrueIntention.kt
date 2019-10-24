// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RIdentifierExpression

class TtoTrueIntention : AbstractRIntention() {

  public override val elementPredicate: PsiElementPredicate
    get() = MergeElseIfPredicate()

  @Throws(IncorrectOperationException::class)
  public override fun processIntention(element: PsiElement, project: Project, editor: Editor) {
    if (element.text == "T") {
      val replacement = element.replace(RElementFactory.createRPsiElementFromText(element.project, "TRUE"))
      editor.caretModel.moveToOffset(replacement.textOffset + 4)
    }

    if (element.text == "F") {
      val replacement = element.replace(RElementFactory.createRPsiElementFromText(element.project, "FALSE"))
      editor.caretModel.moveToOffset(replacement.textOffset + 5)
    }
  }

  override fun getText(): String {
    return "Convert T/F to TRUE and FALSE"
  }

  override fun getFamilyName(): String {
    return "R best practices"
  }

  private class MergeElseIfPredicate : PsiElementPredicate {
    override fun satisfiedBy(element: PsiElement): Boolean {
      return element is RIdentifierExpression && listOf("T", "F").contains(element.getText())
    }
  }
}