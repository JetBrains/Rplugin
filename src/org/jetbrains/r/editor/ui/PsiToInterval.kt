package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.NotebookIntervalPointer
import com.intellij.notebooks.visualization.NotebookIntervalPointerFactory


/**
 * preserves correct mapping between psiElement and NotebookCellLines.Interval even when document is changing
 */
class PsiToInterval(project: Project, editor: EditorImpl, extractPsi: (NotebookCellLines.Interval) -> PsiElement?) {
  private val psiToInterval = LinkedHashMap<PsiElement, NotebookIntervalPointer>()

  init {
    val pointerFactory = NotebookIntervalPointerFactory.get(editor)

    fun scheduleUpdateForCommittedDocument() {
      PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
        psiToInterval.clear()
        if (!editor.isDisposed) {
          for (interval in NotebookCellLines.get(editor).intervals) {
            extractPsi(interval)?.let { psi ->
              psiToInterval[psi] = pointerFactory.create(interval)
            }
          }
        }
      }
    }

    scheduleUpdateForCommittedDocument()

    editor.document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        scheduleUpdateForCommittedDocument()
      }
    }, editor.disposable)
  }

  operator fun get(psiElement: PsiElement): NotebookCellLines.Interval? =
    psiToInterval[psiElement]?.get()

  fun findPsi(interval: NotebookCellLines.Interval): PsiElement? =
    psiToInterval.entries.find { it.value.get() == interval }?.key
}