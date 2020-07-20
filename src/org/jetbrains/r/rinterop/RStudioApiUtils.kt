package org.jetbrains.r.rinterop

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList

enum class RStudioApiFunctionId {
  GET_SOURCE_EDITOR_CONTEXT_ID,
  INSERT_TEXT_ID,
  SEND_TO_CONSOLE_ID,
  GET_CONSOLE_EDITOR_CONTEXT_ID;

  companion object {
    fun fromInt(a: Int): RStudioApiFunctionId {
      return when (a) {
        0 -> GET_SOURCE_EDITOR_CONTEXT_ID
        1 -> INSERT_TEXT_ID
        2 -> SEND_TO_CONSOLE_ID
        3 -> GET_CONSOLE_EDITOR_CONTEXT_ID
        else -> throw IllegalArgumentException("Unknown function id")
      }
    }
  }
}

fun getSourceEditorContext(rInterop: RInterop): RObject {
  return getDocumentContext(rInterop, ContextType.SOURCE)
}

fun getConsoleEditorContext(rInterop: RInterop): RObject {
  return getDocumentContext(rInterop, ContextType.CONSOLE)
}

private enum class ContextType {
  CONSOLE,
  SOURCE
}

private fun getDocumentContext(rInterop: RInterop, type: ContextType): RObject {
  val (document, content, path) = when (type) {
    ContextType.SOURCE -> {
      val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor ?: return RObject.getDefaultInstance()
      val file = editor.file ?: return RObject.getDefaultInstance()
      val document = FileDocumentManager.getInstance().getDocument(file) ?: return RObject.getDefaultInstance()
      val content = file.inputStream.bufferedReader(file.charset).lines().toList()
      val id = file.path
      Triple(document, content, id)
    }
    ContextType.CONSOLE -> {
      val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull ?: return RObject.getDefaultInstance()
      val document = currentConsole.consoleEditor.document
      val content = document.text.split(System.lineSeparator())
      Triple(document, content, "")
    }
  }
  val editors = EditorFactory.getInstance().editors(document, rInterop.project)
  val selections = editors.toList().map { e ->
    e.caretModel.allCarets.filter { it.hasSelection() }.map {
      val startLine = document.getLineNumber(it.selectionStart)
      val endLine = document.getLineNumber(it.selectionEnd)
      val documentPosition = RObject.newBuilder().setRInt(RObject.RInt.newBuilder().addAllInts(listOf(
        (startLine + 1).toLong(),
        (it.selectionStart - document.getLineStartOffset(startLine) + 1).toLong(),
        (endLine + 1).toLong(),
        (it.selectionEnd - document.getLineStartOffset(endLine) + 1).toLong()
      ))).build()
      RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(listOf(
        documentPosition,
        it.selectedText!!.toRString()
      ))).build()
    }
  }.flatten().toRList()
  return RObject.newBuilder()
    .setNamedList(RObject.NamedList.newBuilder()
                    .addRObjects(0, RObject.KeyValue.newBuilder().setKey("id").setValue(getRNull()))
                    .addRObjects(1, RObject.KeyValue.newBuilder().setKey("path").setValue(path.toRString()))
                    .addRObjects(2, RObject.KeyValue.newBuilder().setKey("contents").setValue(
                      RObject.newBuilder().setRString(RObject.RString.newBuilder().addAllStrings(content))))
                    .addRObjects(3, RObject.KeyValue.newBuilder().setKey("selection").setValue(selections)))
    .build()
}

fun insertText(rInterop: RInterop, args: RObject): RObject {
  val document = if (args.list.getRObjects(1).hasRnull()) {
    val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor ?: return RObject.getDefaultInstance()
    val file = editor.file ?: return RObject.getDefaultInstance()
    FileDocumentManager.getInstance().getDocument(file) ?: return RObject.getDefaultInstance()
  }
  else {
    val id = args.list.getRObjects(1).rString.getStrings(0)
    if (id == "") {
      val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull ?: return RObject.getDefaultInstance()
      currentConsole.consoleEditor.document
    }
    else {
      val file = LocalFileSystem.getInstance().findFileByPath(id) ?: return RObject.getDefaultInstance()
      FileDocumentManager.getInstance().getDocument(file) ?: return RObject.getDefaultInstance()
    }
  }
  val insertions = args.list.getRObjects(0).list.rObjectsList.sortedWith(compareBy(
    { -it.list.getRObjects(0).rInt.intsList[0].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[1].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[2].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[3].toInt() }
  ))
  for (insertion in insertions) {
    val range = insertion.list.getRObjects(0).rInt.intsList
    WriteCommandAction.runWriteCommandAction(rInterop.project) {
      val (startPos, endPos) = listOf(0, 2).map {
        if (document.getLineEndOffset(range[it].toInt()) <
            range[it + 1].toInt()) {
          document.getLineEndOffset(range[it].toInt())
        }
        else {
          range[it + 1].toInt()
        }
      }
      document.replaceString(
        document.getLineStartOffset(range[0].toInt()) +
        startPos,
        document.getLineStartOffset(range[2].toInt()) +
        endPos,
        insertion.list.getRObjects(1).rString.getStrings(0)
      )
    }
  }
  return RObject.getDefaultInstance()
}

private val sendToConsoleHelper = AtomicBoolean(false)

fun sendToConsole(rInterop: RInterop, args: RObject): Promise<Unit> {
  val code = args.list.getRObjects(0).rString.getStrings(0)
  val execute = args.list.getRObjects(1).rboolean.getBooleans(0)
  val echo = args.list.getRObjects(2).rboolean.getBooleans(0)
  val focus = args.list.getRObjects(3).rboolean.getBooleans(0)

  val asStr = { it: Boolean -> if (it) "TRUE" else "FALSE" }

  val result = if (echo && !sendToConsoleHelper.get()) {
    sendToConsoleHelper.set(true)
    val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull
    if (currentConsole != null) {
      currentConsole.executeText(
        "rstudioapi::sendToConsole(\"$code\", ${asStr(execute)}, ${asStr(echo)}, ${asStr(focus)})")
    }
    else {
      val promise = AsyncPromise<Unit>()
      promise.setResult(Unit)
      promise
    }
  }
  else {
    sendToConsoleHelper.set(false)
    if (execute) {
      val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull
      if (currentConsole != null) {
        currentConsole.executeText(code)
      }
      else {
        val promise = AsyncPromise<Unit>()
        promise.setResult(Unit)
        promise
      }
    }
    else {
      val promise = AsyncPromise<Unit>()
      invokeLater {
        val consoleEditor = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.consoleEditor
        consoleEditor?.let {
          runWriteAction {
            it.document.setText(code)
            PsiDocumentManager.getInstance(rInterop.project).commitDocument(it.document)
          }
          it.caretModel.moveToOffset(it.document.textLength)
        }
        promise.setResult(Unit)
      }
      promise
    }
  }
  if (focus) {
    //TODO
  }
  return result
}

private fun String.toRString(): RObject {
  return RObject.newBuilder().setRString(RObject.RString.newBuilder().addStrings(this)).build()
}

private fun <T : Iterable<RObject>> T.toRList(): RObject {
  return RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(this)).build()
}

private fun getRNull(): RObject {
  return RObject.newBuilder().setRnull(RObject.RNull.getDefaultInstance()).build()
}