package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.runInEdtAndWait

const val caretToken = "<caret>"

fun extractTextAndCaretOffset(text: String): Pair<String, Int?> {
  val caretOffset = text.indexOf(caretToken)
  if (caretOffset != -1) {
    return Pair(text.substring(0, caretOffset) + text.substring(caretOffset + caretToken.length), caretOffset)
  }
  else {
    return Pair(text, null)
  }
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