package org.jetbrains.r.visualization

import com.intellij.openapi.editor.event.DocumentEvent

/**
 * Passed to [RNotebookCellLines.IntervalListener] before document change.
 * [modificationStamp] is old, version before change
 */
data class RNotebookCellLinesEventBeforeChange(
  val documentEvent: DocumentEvent,
  val oldAffectedIntervals: List<RNotebookCellLines.Interval>,
  val modificationStamp: Long,
)