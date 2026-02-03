// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.run.debug

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.psi.api.RBlockExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.rinterop.RSourceFileManager
import com.intellij.r.psi.rmarkdown.RMarkdownFileType

/*internal*/ object RLineBreakpointUtils {
  fun canPutAt(project: Project, file: VirtualFile, line: Int): Boolean {
    return (FileTypeRegistry.getInstance().isFileOfType(file, RFileType) || FileTypeRegistry.getInstance().isFileOfType(file, RMarkdownFileType)) &&
           !RSourceFileManager.isInvalid(file.url) &&
           isStoppable(project, file, line, !RSourceFileManager.isTemporary(file))
  }

  private fun isStoppable(project: Project, file: VirtualFile, line: Int, allowTopLevel: Boolean = true): Boolean {
    val psiFile = PsiManager.getInstance(project).findViewProvider(file)?.getPsi(RLanguage.INSTANCE) ?: return false
    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return false
    val lineStart = document.getLineStartOffset(line)
    var result = false
    iterateLine(project, psiFile, line) {
      if (it is PsiWhiteSpace || it is PsiComment) return@iterateLine true
      var element: PsiElement? = it
      while (element != null) {
        if (element.textRange.startOffset < lineStart || element is PsiFile) break
        val parent = element.parent
        if (element is RExpression && ((parent is RFile && allowTopLevel) || parent is RBlockExpression)) {
          result = true
          return@iterateLine false
        }
        element = parent
      }
      true
    }
    return result
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
