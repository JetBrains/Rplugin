package org.jetbrains.r.editor

import com.intellij.lang.Language
import com.intellij.openapi.editor.impl.EditorImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCollection
import org.jetbrains.r.visualization.RNotebookCellLines
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.RNotebookCellLinesEvent
import kotlin.reflect.KProperty1

internal class RCodeCellLinesChecker(
  private val description: String,
  private val intervalsComparator: Comparator<Interval> = makeIntervalComparatorIgnoringData(),
  private val editorGetter: () -> EditorImpl,
) : (RCodeCellLinesChecker.() -> Unit) -> Unit {

  private var intervals: MutableList<Interval>? = null
  private var intervalsStartLine: Int = 0
  private val expectedIntervalListenerCalls = mutableListOf<Pair<List<Interval>, List<Interval>>>()


  class IntervalsSetter(private val list: MutableList<Interval>, private val startOrdinal: Int) {
    fun interval(
      cellType: RNotebookCellLines.CellType,
      lines: IntRange,
      markers: RNotebookCellLines.MarkersAtLines,
      language: Language,
    ) {
      list += Interval(list.size + startOrdinal, cellType, lines, markers, language)
    }
  }

  fun intervals(startLine: Int = 0, startOrdinal: Int = 0, handler: IntervalsSetter.() -> Unit) {
    intervalsStartLine = startLine
    check(intervals == null) { "intervals{} section defined twice" }
    intervals = mutableListOf()
    IntervalsSetter(intervals!!, startOrdinal).handler()
  }

  class IntervalListenerCalls(
    private val startOrdinal: Int,
    private val before: MutableList<Interval>,
    private val after: MutableList<Interval>,
  ) {
    fun before(handler: IntervalsSetter.() -> Unit) {
      IntervalsSetter(before, startOrdinal).handler()
    }

    fun after(handler: IntervalsSetter.() -> Unit) {
      IntervalsSetter(after, startOrdinal).handler()
    }
  }

  fun intervalListenerCall(startOrdinal: Int, handler: IntervalListenerCalls.() -> Unit) {
    val before = mutableListOf<Interval>()
    val after = mutableListOf<Interval>()
    IntervalListenerCalls(startOrdinal, before, after).handler()
    expectedIntervalListenerCalls += before to after
  }

  override fun invoke(handler: RCodeCellLinesChecker.() -> Unit) {
    val actualIntervalListenerCalls = mutableListOf<Pair<List<Interval>, List<Interval>>>()
    val intervalListener = object : RNotebookCellLines.IntervalListener {
      override fun documentChanged(event: RNotebookCellLinesEvent) {
        if (event.isIntervalsChanged()) {
          actualIntervalListenerCalls += event.oldIntervals to event.newIntervals
        }
      }
    }
    val editor = editorGetter()
    val codeCellLines = RNotebookCellLines.get(editor.document)
    codeCellLines.intervalListeners.addListener(intervalListener)
    val prettyDocumentTextBefore = editorGetter().prettyText

    try {
      handler()
    }
    catch (err: Throwable) {
      val message =
        try {
          "$err: ${err.message}\nDocument is: ${editorGetter().prettyText}"
        }
        catch (ignored: Throwable) {
          throw err
        }
      throw IllegalStateException(message, err)
    }
    finally {
      codeCellLines.intervalListeners.removeListener(intervalListener)
    }

    val prettyDocumentTextAfter = editorGetter().prettyText

    for (attempt in 0..1) {
      val descr = """
        |||$description${if (attempt > 0) " (repeat to check idempotence)" else ""}
        |||Document before: $prettyDocumentTextBefore
        |||Document after: $prettyDocumentTextAfter
        """.trimMargin("|||")

      intervals?.let { intervals ->
        assertThatCollection(codeCellLines.snapshot.intervalsIteratorByLine(intervalsStartLine).asSequence().toList())
          .describedAs("Intervals: $descr")
          .usingElementComparator(intervalsComparator.toJavaComparatorNonNullable())
          .isEqualTo(intervals)
      }
    }

    // actually this should match intervalsComparator
    fun toPrettyString(interval: Interval): String {
      fun <T> field(property: KProperty1<Interval, T>): String =
        "${property.name} = ${property.get(interval)}"

      return listOf(
        field(Interval::ordinal),
        field(Interval::type),
        field(Interval::lines),
        field(Interval::markers),
        field(Interval::language),
      ).joinToString(", ", prefix = "Interval(", postfix = ")")
    }

    fun List<Pair<List<Interval>, List<Interval>>>.prettyListeners() =
      withIndex().joinToString("\n\n") { (idx, pair) ->
        """
        Call #$idx
          Before:
        ${pair.first.joinToString { "    ${toPrettyString(it)}" }}
          After:
        ${pair.second.joinToString { "    ${toPrettyString(it)}" }}
        """.trimIndent()
      }

    assertThat(actualIntervalListenerCalls.prettyListeners())
      .describedAs("""
        |||Calls of IntervalListener: $description
        |||Document before: $prettyDocumentTextBefore
        |||Document after: $prettyDocumentTextAfter
        """.trimMargin("|||"))
      .isEqualTo(expectedIntervalListenerCalls.prettyListeners())
  }

  companion object {
    fun makeIntervalComparatorIgnoringData(): Comparator<Interval> =
      compareBy<Interval> { it.ordinal }
        .thenBy { it.type }
        .thenBy { it.lines.first }
        .thenBy { it.lines.last }
        .thenBy { it.markers }
        .thenBy { it.language.id }

    // workaround for kotlin type system
    private fun <T> Comparator<T>.toJavaComparatorNonNullable(): java.util.Comparator<Any?> =
      object : java.util.Comparator<Any?> {
        override fun compare(o1: Any?, o2: Any?): Int {
          return this@toJavaComparatorNonNullable.compare(o1 as T, o2 as T)
        }
      }
  }
}

