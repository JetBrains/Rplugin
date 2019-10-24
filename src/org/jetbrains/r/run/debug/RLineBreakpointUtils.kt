// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.rmarkdown.OUTER_ELEMENT
import org.jetbrains.r.rmarkdown.RMarkdownFileType

internal object RLineBreakpointUtils {
  fun canPutAt(project: Project, file: VirtualFile, line: Int): Boolean {
    return (file.fileType == RFileType || file.fileType == RMarkdownFileType) && isStoppable(project, file, line)
  }

  private fun isStoppable(project: Project, file: VirtualFile, line: Int): Boolean {
    val psiFile = PsiManager.getInstance(project).findViewProvider(file)?.getPsi(RLanguage.INSTANCE) ?: return false
    var result = false
    iterateLine(project, psiFile, line) {
      if (isNotStoppable(it) || isNotStoppable(it.node.elementType)) return@iterateLine true
      result = true
      false
    }
    return result
  }

  private fun isNotStoppable(element: PsiElement): Boolean {
    return element is PsiWhiteSpace || element is PsiComment
  }

  private fun isNotStoppable(type: IElementType): Boolean {
    return type === RElementTypes.R_LBRACE || type === RElementTypes.R_RBRACE || type === OUTER_ELEMENT
  }

  private inline fun iterateLine(project: Project, file: PsiFile, line: Int, processor: (PsiElement) -> Boolean) {
    val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
    val viewProvider = file.viewProvider
    val lineStart: Int
    val lineEnd: Int
    try {
      lineStart = document.getLineStartOffset(line)
      lineEnd = document.getLineEndOffset(line)
    } catch (ignored: IndexOutOfBoundsException) {
      return
    }
    var offset = lineStart
    while (offset < lineEnd) {
      val element = viewProvider.findElementAt(offset, RLanguage.INSTANCE)
      if (element != null && element.textLength > 0) {
        if (!processor(element)) return
        offset = element.textRange.endOffset
      } else {
        offset++
      }
    }
  }
}
