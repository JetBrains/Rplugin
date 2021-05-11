package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager

private const val ID: String = "org.jetbrains.plugins.notebooks.notebookCellLinesProvider"

interface NotebookCellLinesProvider {
  fun create(document: Document): NotebookCellLines

  companion object : LanguageExtension<NotebookCellLinesProvider>(ID)
}

internal val Editor.notebookCellLinesProvider: NotebookCellLinesProvider?
  get() = project
    ?.let(PsiDocumentManager::getInstance)
    ?.getPsiFile(document)
    ?.language
    ?.let(NotebookCellLinesProvider::forLanguage)