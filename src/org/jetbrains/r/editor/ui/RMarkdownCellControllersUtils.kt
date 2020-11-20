package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile

internal fun offset(document: Document, codeLines: IntRange): Int =
  Integer.min(document.getLineEndOffset(codeLines.first), document.textLength)

internal fun isRMarkdown(editor: Editor): Boolean =
  editor.psiFile is MarkdownFile

internal val Editor.psiFile: PsiFile?
  get() {
    return PsiDocumentManager.getInstance(project ?: return null).getPsiFile(document)
  }

internal fun getFenceLangByOffset(psiFile: PsiFile, offset: Int): PsiElement? =
  psiFile.viewProvider
    .let { it.findElementAt(offset, it.baseLanguage) }
    ?.let { it.parent.children.find { it.elementType == MarkdownTokenTypes.FENCE_LANG } }