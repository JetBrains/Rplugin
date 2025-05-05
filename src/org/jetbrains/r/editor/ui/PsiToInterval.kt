package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.r.visualization.RNotebookCellLines
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.RNotebookIntervalPointer
import org.jetbrains.r.visualization.RNotebookIntervalPointerFactory


/**
 * preserves correct mapping between psiElement and NotebookCellLines.Interval even when document is changing
 */
class PsiToInterval(project: Project, editor: EditorImpl, extractPsi: (Interval) -> PsiElement?) {
  private val psiToInterval = LinkedHashMap<PsiElement, RNotebookIntervalPointer>()

  init {
    val pointerFactory = RNotebookIntervalPointerFactory.get(editor)

    fun scheduleUpdateForCommittedDocument() {
      PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
        psiToInterval.clear()
        if (!editor.isDisposed) {
          for (interval in RNotebookCellLines.getSnapshot(editor.document).intervals) {
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

  operator fun get(psiElement: PsiElement): Interval? =
    psiToInterval[psiElement]?.get()

  fun findPsi(interval: Interval): PsiElement? =
    psiToInterval.entries.find { it.value.get() == interval }?.key
}