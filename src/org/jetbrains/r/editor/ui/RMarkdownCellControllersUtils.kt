package org.jetbrains.r.editor.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile

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

internal fun performForCommittedPsi(editor: Editor, inlay: Inlay<*>, processPsiElement: (PsiElement) -> Unit) {
  PsiDocumentManager.getInstance(editor.project!!).performForCommittedDocument(editor.document) {
    val psiElement = getFenceLangByOffset(editor.psiFile!!, inlay.offset)
    if (psiElement != null) {
      processPsiElement(psiElement)
    }
    else {
      if (editor.document.textLength > 0) {
        Logger.getInstance("#org.jetbrains.r.editor.ui.performOnCommittedPsi")
          .error("psi structure and RMarkdownMergingLangLexer result don't match")
      }
      Disposer.dispose(inlay)
    }
  }
}
