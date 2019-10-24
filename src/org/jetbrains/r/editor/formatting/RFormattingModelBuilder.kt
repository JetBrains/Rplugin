// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.DocumentBasedFormattingModel

class RFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
    val containingFile = element.containingFile
    val astNode = containingFile.node!!

    val root = RFormatterBlock(RFormattingContext(settings), astNode)
    return DocumentBasedFormattingModel(root, settings, containingFile)
  }

  override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? {
    return null
  }
}
