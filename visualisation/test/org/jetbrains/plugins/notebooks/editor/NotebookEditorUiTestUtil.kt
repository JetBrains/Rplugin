package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.SmartList
import java.util.regex.Pattern


private const val caretToken = "<caret>"
private const val selectionBegin = "<selection>"
private const val selectionEnd = "</selection>"
private const val foldBegin = "<fold>"
private const val foldEnd = "</fold>"


data class CaretWithSelection(val caret: Int, val selection: TextRange) {
  constructor(caret: Int) : this(caret, TextRange(caret, caret))
}

data class ExtractedInfo(val text: String, val carets: List<CaretWithSelection>, val folds: List<TextRange>) {
  fun setCaretsInEditor(editor: Editor) {
    editor.caretModel.runBatchCaretOperation {
      for(offset in 0 until editor.document.textLength) {
        if (editor.caretModel.caretCount >= carets.size) {
          break
        }
        editor.caretModel.addCaret(editor.offsetToVisualPosition(offset), false)
      }

      for((editorCaret, caretInfo) in editor.caretModel.allCarets.zip(carets)) {
        editorCaret.moveToOffset(caretInfo.caret)
        editorCaret.setSelection(caretInfo.selection.startOffset, caretInfo.selection.endOffset)
      }
    }
  }

  fun setFoldingsInEditor(editor: Editor) {
    editor.foldingModel.runBatchFoldingOperation {
      for (foldRange in folds) {
        editor.foldingModel.addFoldRegion(foldRange.startOffset, foldRange.endOffset, "folded")?.also {
          it.isExpanded = false
        }
      }
    }
  }
}

fun extractTextAndCaretOffset(text: String): Pair<String, Int?> {
  val caretOffset = text.indexOf(caretToken)
  if (caretOffset != -1) {
    return Pair(text.substring(0, caretOffset) + text.substring(caretOffset + caretToken.length), caretOffset)
  }
  else {
    return Pair(text, null)
  }
}

fun extractCaretsAndFoldings(text: String): ExtractedInfo {
  val tagTokens = listOf(caretToken, selectionBegin, selectionEnd, foldBegin, foldEnd)
  val (textWithoutTags, tags) = extractCarets(text, findAllTags(text, tagTokens))
  return ExtractedInfo(textWithoutTags, extractCaretsInfo(tags), extractFoldings(tags))
}

/* add caretToken into the text */
val Editor.prettyText: String
  get() = document
    .text
    .lineSequence()
    .withIndex()
    .fold(0 to StringBuilder()) { (offset, text), (lineNumber, line) ->
      val lineWithCaret =
        caretModel
          .logicalPosition
          .takeIf { it.line == lineNumber }
          ?.column
          ?.let { line.substring(0, it) + caretToken + line.substring(it) }
        ?: line
      text.append("\nLine ${lineNumber.toString().padStart(2)} Offset ${offset.toString().padStart(3)}: $lineWithCaret")
      offset + line.length + 1 to text
    }
    .second
    .toString()

fun edt(runnable: () -> Unit) {
  runInEdtAndWait(runnable)
}

val Editor.textWithCarets: String
  get() {
    val tags = extractCarets(this)
    return insertTags(document.text, tags)
  }

private fun extractCarets(editor: Editor): List<Pair<String, Int>> {
  val tags = SmartList<Pair<String, Int>>()

  for (caret in editor.caretModel.allCarets) {
    if (caret.hasSelection()) {
      tags.add(Pair(selectionBegin, caret.selectionStart))
      tags.add(Pair(caretToken, caret.offset))
      tags.add(Pair(selectionEnd, caret.selectionEnd))
    }
    else tags.add(Pair(caretToken, caret.offset))
  }

  return tags
}

private fun insertTags(text: String, orderedTags: List<Pair<String, Int>>): String {
  val result = StringBuilder()

  var end = 0
  for ((tagText, tagPos) in orderedTags) {
    result.append(text.subSequence(end, tagPos))
    result.append(tagText)
    end = tagPos
  }
  result.append(text.subSequence(end, text.length))

  return result.toString()
}

private fun findAllTags(text: String, tags: List<String>): List<TextRange> {
  val regex = tags.joinToString(separator = "|", prefix = "(", postfix = ")") { Pattern.quote(it) }
  val p = Pattern.compile(regex)
  val m = p.matcher(text)

  val tagsInText = SmartList<TextRange>()
  while (m.find()) {
    tagsInText.add(TextRange(m.start(), m.end()))
  }
  return tagsInText
}

private fun extractCarets(text: String, ranges: List<TextRange>): Pair<String, List<Pair<String, Int>>> {
  val textWithoutTags = StringBuilder()
  val tags = SmartList<Pair<String, Int>>()

  var start = 0
  var tagsTextLength = 0
  for (range in ranges) {
    textWithoutTags.append(text.subSequence(start, range.startOffset))
    tags.add(Pair(text.substring(range.startOffset, range.endOffset), range.startOffset - tagsTextLength))
    start = range.endOffset
    tagsTextLength += range.length
  }
  textWithoutTags.append(text.subSequence(start, text.length))

  return Pair(textWithoutTags.toString(), tags)
}

private fun extractCaretsInfo(tags: List<Pair<String, Int>>): List<CaretWithSelection> {
  val caretsTags = tags.filter { it.first in setOf(caretToken, selectionBegin, selectionEnd) }
  val result = SmartList<CaretWithSelection>()

  var tagNo = 0
  fun next(): Pair<String, Int> = caretsTags[tagNo++]

  fun matchSeq(vararg seq: String): Boolean =
    seq.withIndex().all { (index, s) -> caretsTags.getOrNull(index + tagNo)?.first == s }

  while (tagNo < caretsTags.size) {
    when {
      matchSeq(caretToken) -> result.add(CaretWithSelection(next().second))
      matchSeq(selectionBegin, selectionEnd) -> {
        val (_, openPos) = next()
        val (_, closePos) = next()
        result.add(CaretWithSelection(closePos, TextRange(openPos, closePos)))
      }
      matchSeq(selectionBegin, caretToken, selectionEnd) -> {
        val (_, openPos) = next()
        val (_, caretPos) = next()
        val (_, closePos) = next()
        result.add(CaretWithSelection(caretPos, TextRange(openPos, closePos)))
      }
      else -> error("can't match end of carets sequence ${caretsTags.drop(tagNo).joinToString(",")}")
    }
  }

  return result
}

private fun extractFoldings(tags: List<Pair<String, Int>>): List<TextRange> {
  val result = SmartList<TextRange>()
  val foldingsTags = tags.filter { it.first in setOf(foldBegin, foldEnd) }
  var tagNo = 0
  while (tagNo < foldingsTags.size) {
    if (tagNo + 1 < foldingsTags.size && foldingsTags[tagNo].first == foldBegin && foldingsTags[tagNo + 1].first == foldEnd) {
      result.add(TextRange(foldingsTags[tagNo].second, foldingsTags[tagNo + 1].second))
      tagNo += 2
    }
    else {
      error("can't match end of foldings sequence ${foldingsTags.drop(tagNo)}")
    }
  }
  return result
}