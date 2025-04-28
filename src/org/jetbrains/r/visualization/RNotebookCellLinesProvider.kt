package org.jetbrains.r.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import org.jetbrains.r.editor.RMarkdownIntervalsGenerator

object RNotebookCellLinesProvider {
  private val key = Key.create<RNotebookCellLines>(RNotebookCellLinesProvider::class.java.name)

  fun get(document: Document): RNotebookCellLines {
    val alreadyInitialized = document.getUserData(key)
    if (alreadyInitialized != null) return alreadyInitialized

    synchronized(this) {
      val syncInitialized = document.getUserData(key)
      if (syncInitialized != null) return syncInitialized

      return create(document).also {
        document.putUserData(key, it)
      }
    }
  }

  private fun create(document: Document): RNotebookCellLines =
    RNonIncrementalCellLines.create(document, RMarkdownIntervalsGenerator)
}