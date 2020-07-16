package org.jetbrains.r.rinterop

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import kotlin.streams.toList

const val GET_SOURCE_EDITOR_CONTEXT_ID = 0

fun getSourceEditorContext(rInterop: RInterop): RObject {
  val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor ?: TODO("editor is null")
  val file = editor.file ?: TODO("file is null")
  val document = FileDocumentManager.getInstance().getDocument(file) ?: TODO("document is null")
  val content = file.inputStream.bufferedReader(file.charset).lines()
  val editors = EditorFactory.getInstance().editors(document, rInterop.project)
  val selections = editors.toList().filter { it.selectionModel.selectedText != null }.map {
    val code = it.selectionModel.selectedText!!
    val startLine = document.getLineNumber(it.selectionModel.selectionStart)
    val endLine = document.getLineNumber(it.selectionModel.selectionEnd)
    RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(listOf(
      (startLine + 1).toRInt(),
      (it.selectionModel.selectionStart - document.getLineStartOffset(startLine) + 1).toRInt(),
      (endLine + 1).toRInt(),
      (it.selectionModel.selectionEnd - document.getLineStartOffset(endLine) + 1).toRInt(),
      code.toRString()
    ))).build()
  }.toRList()
  return RObject.newBuilder()
    .setList(RObject.List.newBuilder()
               .addRObjects(0, file.path.toRString())
               .addRObjects(1, file.path.toRString())
               .addRObjects(2, content.map { it.toRString() }.toList().toRList())
               .addRObjects(3, selections))
    .build()
}

fun Int.toRInt(): RObject {
  return RObject.newBuilder().setRInt(RObject.RInt.newBuilder().setInt(this.toLong())).build()
}

fun String.toRString(): RObject {
  return RObject.newBuilder().setRString(RObject.RString.newBuilder().setString(this)).build()
}

fun <T: Iterable<RObject>> T.toRList(): RObject {
  return RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(this)).build()
}