package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiManager
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import com.intellij.util.PathUtilRt
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import kotlin.streams.toList

fun getSourceEditorContext(rInterop: RInterop): RObject {
  return getDocumentContext(rInterop, ContextType.SOURCE)
}

fun getConsoleEditorContext(rInterop: RInterop): RObject {
  return getDocumentContext(rInterop, ContextType.CONSOLE)
}

fun getActiveDocumentContext(rInterop: RInterop): RObject {
  return getDocumentContext(rInterop, ContextType.ACTIVE)
}

private enum class ContextType {
  CONSOLE,
  SOURCE,
  ACTIVE
}

private fun getDocumentContext(rInterop: RInterop, type: ContextType): RObject {
  val id: String
  val newType = if (type == ContextType.ACTIVE) {
    if (rInterop.isInSourceFileExecution.get()) {
      ContextType.SOURCE
    }
    else ContextType.CONSOLE
  }
  else type
  val (document, content, path) = when (newType) {
    ContextType.SOURCE -> {
      val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor ?: return getRNull()
      val file = editor.file ?: return getRNull()
      val document = FileDocumentManager.getInstance().getDocument(file) ?: return getRNull()
      val content = file.inputStream.bufferedReader(file.charset).lines().toList()
      val path = rInterop.interpreter.getFilePathAtHost(file) ?: return getRNull()
      id = "s${editor.hashCode()}"
      Triple(document, content, path)
    }
    ContextType.CONSOLE, ContextType.ACTIVE -> {
      val console = getConsoleView(rInterop) ?: return getRNull()
      val document = console.consoleEditor.document
      val content = document.text.split(System.lineSeparator())
      id = "c"
      Triple(document, content, "")
    }
  }
  val editors = EditorFactory.getInstance().editors(document, rInterop.project)
  val selections = editors.toList().map { e ->
    e.caretModel.allCarets.map {
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
        (it.selectedText ?: "").toRString()
      ))).build()
    }
  }.flatten().toRList()
  return RObject.newBuilder()
    .setNamedList(RObject.NamedList.newBuilder()
                    .addRObjects(0, RObject.KeyValue.newBuilder().setKey("id").setValue(id.toRString()))
                    .addRObjects(1, RObject.KeyValue.newBuilder().setKey("path").setValue(path.toRString()))
                    .addRObjects(2, RObject.KeyValue.newBuilder().setKey("contents").setValue(
                      RObject.newBuilder().setRString(RObject.RString.newBuilder().addAllStrings(content))))
                    .addRObjects(3, RObject.KeyValue.newBuilder().setKey("selection").setValue(selections)))
    .build()
}

fun insertText(rInterop: RInterop, args: RObject): RObject {
  val document = if (args.list.getRObjects(1).hasRnull()) {
    getDocumentFromId(null, rInterop) ?: return getRNull()
  }
  else {
    val id = args.list.getRObjects(1).rString.getStrings(0)
    getDocumentFromId(id, rInterop) ?: return getRNull()
  }
  val insertions = args.list.getRObjects(0).list.rObjectsList.sortedWith(compareBy(
    { -it.list.getRObjects(0).rInt.intsList[0].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[1].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[2].toInt() },
    { -it.list.getRObjects(0).rInt.intsList[3].toInt() }
  ))

  // Insert at the current selection
  if (insertions.size == 1 && insertions[0].list.getRObjects(0).rInt.intsList.all { it == -1L }) {
    val editor = EditorFactory.getInstance().editors(document, rInterop.project).toList().first() ?: return getRNull()
    WriteCommandAction.runWriteCommandAction(rInterop.project) {
      document.replaceString(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd,
                             insertions[0].list.getRObjects(1).rString.getStrings(0))
    }
    return getRNull()
  }

  for (insertion in insertions) {
    val range = insertion.list.getRObjects(0).rInt.intsList
    WriteCommandAction.runWriteCommandAction(rInterop.project) {
      when {
        range[0] >= document.lineCount -> {
          document.insertString(document.getLineEndOffset(if (document.lineCount == 0) 0 else document.lineCount - 1),
                                insertion.list.getRObjects(1).rString.getStrings(0))
        }
        range[2] >= document.lineCount -> {
          document.setText(insertion.list.getRObjects(1).rString.getStrings(0))
        }
        else -> {
          val (startPos, endPos) = listOf(0, 2).map {
            if (document.getLineEndOffset(range[it].toInt()) - document.getLineStartOffset(range[it].toInt()) <
                range[it + 1].toInt()) {
              document.getLineEndOffset(range[it].toInt()) - document.getLineStartOffset(range[it].toInt())
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
    }
  }
  return getRNull()
}

fun setSelectionRanges(rInterop: RInterop, args: RObject): RObject {
  val ranges = args.list.getRObjects(0).list.rObjectsList.map { it.rInt.intsList.map { it.toInt() } }
  val document = if (args.list.getRObjects(1).hasRnull()) {
    getDocumentFromId(null, rInterop)
  } else {
    val id = args.list.getRObjects(1).rString.getStrings(0)
    getDocumentFromId(id, rInterop)
  } ?: return getRNull()
  for (r in ranges) {
    val editors = EditorFactory.getInstance().editors(document, rInterop.project).toList()
    editors.map {
      it.caretModel.addCaret(
        VisualPosition(r[2], r[3])
      )?.setSelection(
        document.getLineStartOffset(r[0]) + r[1],
        document.getLineStartOffset(r[2]) + r[3]
      )
    }
  }
  return getRNull()
}

fun documentNew(rInterop: RInterop, args: RObject): Promise<Unit> {
  val type = args.list.getRObjects(0).rString.getStrings(0)
  val text = args.list.getRObjects(1).rString.getStrings(0)
  val line = args.list.getRObjects(2).rInt.getInts(0).toInt()
  val column = args.list.getRObjects(2).rInt.getInts(1).toInt()
  val execute = args.list.getRObjects(3).rboolean.getBooleans(0)
  val fileChooser = rInterop.interpreter.createFileChooserForHost(rInterop.interpreter.basePath, false)
  val panel = panel {
    row { component(fileChooser).focused() }
  }
  val dialogPromise = AsyncPromise<String?>()
  runInEdt {
    val dialog = dialog("Select file for new document", panel)
    val result = if (dialog.showAndGet()) {
      fileChooser.text
    }
    else null
    dialogPromise.setResult(result)
  }
  val promise = AsyncPromise<Unit>()
  dialogPromise.then {
    if (it != null) {
      val newFilePath = rInterop.interpreter.createFileOnHost(it, text.toByteArray(), "")
      val name = PathUtilRt.getFileName(newFilePath)
      ProgressManager.getInstance().run(object : Task.Backgroundable(
        rInterop.project, "remote.host.view.opening.file.title.$name") {
        override fun run(indicator: ProgressIndicator) {
          val file = rInterop.interpreter.findFileByPathAtHost(newFilePath) ?: return
          invokeAndWaitIfNeeded {
            FileTypeChooser.getKnownFileTypeOrAssociate(newFilePath)
            val editor = FileEditorManager.getInstance(rInterop.project)
              .openTextEditor(OpenFileDescriptor(rInterop.project, file, line, column), true)
            editor?.selectionModel?.setSelection(
              editor.visualPositionToOffset(VisualPosition(line, column)),
              editor.visualPositionToOffset(VisualPosition(line, column))
            )
            if (execute) {
              when (type) {
                "r" -> rInterop.replSourceFile(file)
                "rmarkdown" -> invokeLater {
                  if (editor != null) {
                    val psiFile = PsiManager.getInstance(rInterop.project).findFile(file)
                    psiFile?.let {
                      val state = editor.chunkExecutionState
                      if (state == null) {
                        ChunkExecutionState(editor).apply {
                          editor.chunkExecutionState = this
                          RunChunkHandler.runAllChunks(psiFile, editor, currentPsiElement,
                                                       terminationRequired).onProcessed { editor.chunkExecutionState = null }
                        }
                      }
                      else {
                        state.terminationRequired.set(true)
                        val element = state.currentPsiElement.get()
                        RunChunkHandler.interruptChunkExecution(element.project)
                      }
                    }
                  }
                }
                "sql" -> TODO()
                else -> {
                }
              }
            }
          }
          promise.setResult(Unit)
        }
      })
    }
  }
  return promise
}

fun navigateToFile(rInterop: RInterop, args: RObject): RObject {
  val filePath = args.list.getRObjects(0).rString.getStrings(0)
  val line = args.list.getRObjects(1).rInt.getInts(0).toInt() - 1
  val column = args.list.getRObjects(1).rInt.getInts(1).toInt() - 1
  val file = rInterop.interpreter.findFileByPathAtHost(filePath) ?: return rError("$filePath does not exist.")
  FileEditorManager.getInstance(rInterop.project)
    .openTextEditor(OpenFileDescriptor(rInterop.project, file, line, column), true)
  return getRNull()
}

private fun getDocumentFromId(id: String?, rInterop: RInterop): Document? {
  if (id == null) {
    return if (rInterop.isInSourceFileExecution.get()) {
      val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor
      val file = editor?.file
      file?.let { FileDocumentManager.getInstance().getDocument(file) }
    }
    else {
      val console = getConsoleView(rInterop)
      console?.editorDocument
    }
  }
  val type = id[0]
  return if (type == 'c') {
    val console = getConsoleView(rInterop)
    console?.consoleEditor?.document
  }
  else {
    val numId = (id.drop(1)).toInt()
    val editor = FileEditorManager.getInstance(rInterop.project).allEditors.find { it.hashCode() == numId }
    val file = editor?.file
    file?.let { FileDocumentManager.getInstance().getDocument(file) }
  }
}


