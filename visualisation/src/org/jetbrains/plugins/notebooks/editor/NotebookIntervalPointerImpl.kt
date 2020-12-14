package org.jetbrains.plugins.notebooks.editor


internal class NotebookIntervalPointerImpl(val factory: NotebookIntervalPointerFactoryImpl, var ordinal: Int?): NotebookIntervalPointer {
  override fun get(): NotebookCellLines.Interval? =
    ordinal?.let{ factory[it] }

  override fun isValid(): Boolean =
    ordinal != null

  fun dispose() {
    ordinal = null
  }
}


internal class NotebookIntervalPointerFactoryImpl(val notebookCellLines: NotebookCellLines): NotebookIntervalPointerFactory, NotebookCellLines.IntervalListener {
  private val pointers = ArrayList<NotebookIntervalPointerImpl>()

  init {
    pointers.addAll(notebookCellLines.intervals.indices.map { ordinal -> NotebookIntervalPointerImpl(this, ordinal) })
    notebookCellLines.intervalListeners.addListener(this)
  }

  internal operator fun get(ordinal: Int): NotebookCellLines.Interval = notebookCellLines.intervals[ordinal]

  override fun create(ordinal: Int): NotebookIntervalPointer =
    pointers.getOrNull(ordinal) ?: NotebookIntervalPointerImpl(this, null)

  override fun segmentChanged(oldIntervals: List<NotebookCellLines.Interval>, newIntervals: List<NotebookCellLines.Interval>) {
    oldIntervals.firstOrNull()?.let { first ->
      newIntervals.firstOrNull()?.let { second ->
        require(first.ordinal == second.ordinal)
      }
    }

    pointers.removeAll(oldIntervals.map {
      pointers[it.ordinal].also { it.dispose() }
    })

    newIntervals.firstOrNull()?.also { firstNew ->
      pointers.addAll(firstNew.ordinal, newIntervals.map { NotebookIntervalPointerImpl(this, it.ordinal) })
      for(i in firstNew.ordinal + newIntervals.size until pointers.size) {
        pointers[i].ordinal = i
      }
    }
  }
}