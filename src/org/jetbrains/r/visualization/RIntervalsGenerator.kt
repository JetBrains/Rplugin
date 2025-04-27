package org.jetbrains.r.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent

internal interface RIntervalsGenerator {
  fun makeIntervals(document: Document, event: DocumentEvent? = null): List<RNotebookCellLines.Interval>
}
