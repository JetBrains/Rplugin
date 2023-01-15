package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretState
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
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.PathUtilRt
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.findFileByPathAtHostHelper
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.getConsoleView
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.getRNull
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.rError
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRBoolean
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRList
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toStringOrNull
import kotlin.streams.toList

object DocumentUtils {
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
    val document = getDocumentFromId(args.list.getRObjects(1).toStringOrNull(), rInterop) ?: return getRNull()
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
      val range = insertion.list.getRObjects(0).rInt.intsList.map { it.toInt() }.let {
        listOf(it[0] to it[1], it[2] to it[3])
      }.sortedWith(compareBy({ it.first }, { it.second }))
      WriteCommandAction.runWriteCommandAction(rInterop.project) {
        val offset = convertRanges(range, document)
        document.replaceString(
          offset.first,
          offset.second,
          insertion.list.getRObjects(1).rString.getStrings(0)
        )
      }
    }
    return getRNull()
  }

  fun setSelectionRanges(rInterop: RInterop, args: RObject): RObject {
    val ranges = args.list.getRObjects(0).list.rObjectsList.map { it.rInt.intsList.map { it.toInt() } }
    val document = getDocumentFromId(args.list.getRObjects(1).toStringOrNull(), rInterop) ?: return getRNull()
    val editors = EditorFactory.getInstance().editors(document, rInterop.project).toList()
    editors.map { editor ->
      editor.caretModel.caretsAndSelections = ranges.map { r ->
        val range = r.let {
          listOf(it[0] to it[1], it[2] to it[3])
        }.sortedWith(compareBy({ it.first }, { it.second }))
        val (selectionStart, selectionEnd) = convertRanges(range, document)
        CaretState(editor.offsetToLogicalPosition(selectionEnd), editor.offsetToLogicalPosition(selectionStart),
                   editor.offsetToLogicalPosition(selectionEnd))
      }
    }
    return getRNull()
  }

  fun documentNew(rInterop: RInterop, args: RObject): Promise<RObject> {
    val type = args.list.getRObjects(0).rString.getStrings(0)
    val text = args.list.getRObjects(1).rString.getStrings(0)
    val line = args.list.getRObjects(2).rInt.getInts(0).toInt()
    val column = args.list.getRObjects(2).rInt.getInts(1).toInt()
    val execute = args.list.getRObjects(3).rBoolean.getBooleans(0)
    val fileChooser = rInterop.interpreter.createFileChooserForHost(rInterop.interpreter.basePath, false)
    val panel = panel {
      row { label(RBundle.message("rstudioapi.new.document.select.file.message")) }
      row { cell(fileChooser).columns(40).focused() }
    }
    val dialogPromise = AsyncPromise<String?>()
    runInEdt {
      val dialog = dialog("", panel)
      val result = if (dialog.showAndGet()) {
        fileChooser.text
      }
      else null
      dialogPromise.setResult(result)
    }
    val promise = AsyncPromise<RObject>()
    dialogPromise.then {
      if (it != null) {
        val path = if (PathUtilRt.getFileExtension(it) == null) {
          it + when (type) {
            "r" -> ".R"
            "rmarkdown" -> ".rmd"
            "sql" -> ".sql"
            else -> ""
          }
        }
        else {
          it
        }
        val newFilePath = rInterop.interpreter.createFileOnHost(path, text.toByteArray(), "")
        ProgressManager.getInstance().run(object : Task.Backgroundable(
          rInterop.project, RBundle.message("rstudioapi.create.new.document")) {
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
                  else -> {
                  }
                }
              }
            }
            promise.setResult(RObject.getDefaultInstance())
          }
        })
      }
      else promise.setResult(rError("No file selected"))
    }
    return promise
  }

  fun navigateToFile(rInterop: RInterop, args: RObject): Promise<RObject> {
    val filePath = args.list.getRObjects(0).rString.getStrings(0)
    val line = args.list.getRObjects(1).rInt.getInts(0).toInt() - 1
    val column = args.list.getRObjects(1).rInt.getInts(1).toInt() - 1
    val promise = AsyncPromise<RObject>()
    val filePromise = findFileByPathAtHostHelper(rInterop, filePath)
    filePromise.then {
      it ?: promise.setResult(rError("$filePath does not exist."))
      runInEdt {
        FileEditorManager.getInstance(rInterop.project)
          .openTextEditor(OpenFileDescriptor(rInterop.project, it!!, line, column), true)
        promise.setResult(getRNull())
      }
    }
    return promise
  }

  fun documentClose(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val numId = (id.drop(1)).toInt()
    val editor = FileEditorManager.getInstance(rInterop.project).allEditors.find { it.hashCode() == numId }
    val file = editor?.file ?: return true.toRBoolean()
    FileEditorManager.getInstance(rInterop.project).closeFile(file)
    return true.toRBoolean()
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

  private fun convertRanges(rng: List<Pair<Int, Int>>, document: Document): Pair<Int, Int> {
    return RStudioApiUtils.getLineOffset(document, rng[0].first, rng[0].second) to
      RStudioApiUtils.getLineOffset(document, rng[1].first, rng[1].second)
  }
}



