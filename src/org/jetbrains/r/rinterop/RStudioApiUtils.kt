package org.jetbrains.r.rinterop

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons.General.QuestionDialog
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.ui.components.*
import com.intellij.ui.layout.*
import com.intellij.util.PathUtil
import com.intellij.util.PathUtilRt
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.jobs.ExportGlobalEnvPolicy
import org.jetbrains.r.console.jobs.RJobRunner
import org.jetbrains.r.console.jobs.RJobTask
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import javax.swing.JPasswordField
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.streams.toList

enum class RStudioApiFunctionId {
  GET_SOURCE_EDITOR_CONTEXT_ID,
  INSERT_TEXT_ID,
  SEND_TO_CONSOLE_ID,
  GET_CONSOLE_EDITOR_CONTEXT_ID,
  NAVIGATE_TO_FILE_ID,
  GET_ACTIVE_PROJECT_ID,
  GET_ACTIVE_DOCUMENT_CONTEXT_ID,
  SET_SELECTION_RANGES_ID,
  ASK_FOR_PASSWORD_ID,
  SHOW_QUESTION_ID,
  SHOW_PROMPT_ID,
  ASK_FOR_SECRET_ID,
  SELECT_FILE_ID,
  SELECT_DIRECTORY_ID,
  SHOW_DIALOG_ID,
  UPDATE_DIALOG_ID,
  GET_THEME_INFO,
  JOB_RUN_SCRIPT_ID,
  JOB_REMOVE_ID,
  JOB_SET_STATE_ID,
  RESTART_SESSION_ID,
  DOCUMENT_NEW_ID;

  companion object {
    fun fromInt(a: Int): RStudioApiFunctionId {
      return when (a) {
        0 -> GET_SOURCE_EDITOR_CONTEXT_ID
        1 -> INSERT_TEXT_ID
        2 -> SEND_TO_CONSOLE_ID
        3 -> GET_CONSOLE_EDITOR_CONTEXT_ID
        4 -> NAVIGATE_TO_FILE_ID
        5 -> GET_ACTIVE_PROJECT_ID
        6 -> GET_ACTIVE_DOCUMENT_CONTEXT_ID
        7 -> SET_SELECTION_RANGES_ID
        8 -> ASK_FOR_PASSWORD_ID
        9 -> SHOW_QUESTION_ID
        10 -> SHOW_PROMPT_ID
        11 -> ASK_FOR_SECRET_ID
        12 -> SELECT_FILE_ID
        13 -> SELECT_DIRECTORY_ID
        14 -> SHOW_DIALOG_ID
        15 -> UPDATE_DIALOG_ID
        16 -> GET_THEME_INFO
        17 -> JOB_RUN_SCRIPT_ID
        18 -> JOB_REMOVE_ID
        19 -> JOB_SET_STATE_ID
        20 -> RESTART_SESSION_ID
        21 -> DOCUMENT_NEW_ID
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

fun sendToConsole(rInterop: RInterop, args: RObject) {
  val code = args.list.getRObjects(0).rString.getStrings(0)
  val execute = args.list.getRObjects(1).rboolean.getBooleans(0)
  val echo = args.list.getRObjects(2).rboolean.getBooleans(0)
  val focus = args.list.getRObjects(3).rboolean.getBooleans(0)
  val call = args.list.getRObjects(4).rString.getStrings(0)

  val console = getConsoleView(rInterop) ?: return

  if (echo) {
    if (rInterop.isInSourceFileExecution.get()) {
      console.executeText(call)
    }
  }

  if (focus) {
    val toolWindow = RConsoleToolWindowFactory.getRConsoleToolWindows(console.project)
    if (toolWindow != null) {
      RConsoleToolWindowFactory.getConsoleContent(console)?.let { content ->
        toolWindow.activate {
          toolWindow.contentManager.setSelectedContent(content)
        }
      }
    }
  }

  if (execute) {
    val text = console.editorDocument.text
    console.executeText(code).then {
      invokeLater {
        val consoleEditor = console.consoleEditor
        runWriteAction {
          consoleEditor.document.setText(text)
          PsiDocumentManager.getInstance(rInterop.project).commitDocument(consoleEditor.document)
        }
        consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
      }
    }
  }
  else {
    invokeLater {
      val consoleEditor = console.consoleEditor
      runWriteAction {
        consoleEditor.document.setText(code)
        PsiDocumentManager.getInstance(rInterop.project).commitDocument(consoleEditor.document)
      }
      consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
    }
  }
}

fun navigateToFile(rInterop: RInterop, args: RObject): RObject {
  val filePath = args.list.getRObjects(0).rString.getStrings(0)
  val line = args.list.getRObjects(1).rInt.getInts(0).toInt() - 1
  val column = args.list.getRObjects(1).rInt.getInts(1).toInt() - 1
  val file = rInterop.interpreter.findFileByPathAtHost(filePath) ?: return getRNull()
  FileEditorManager.getInstance(rInterop.project)
    .openTextEditor(OpenFileDescriptor(rInterop.project, file, line, column), true)
  return getRNull()
}

fun getActiveProject(rInterop: RInterop): RObject {
  val path = rInterop.interpreter.basePath
  return path.toRString()
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

fun askForPassword(args: RObject): Promise<RObject> {
  val message = args.rString.getStrings(0)
  val passwordField = JPasswordField()
  val panel = panel {
    noteRow(message)
    row { passwordField().focused() }
  }
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val dialog = dialog("", panel)
    dialog.isOKActionEnabled = false
    val validate: () -> Unit = {
      dialog.isOKActionEnabled = passwordField.password.isNotEmpty()
    }
    passwordField.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent?) = validate()
      override fun insertUpdate(e: DocumentEvent?) = validate()
      override fun removeUpdate(e: DocumentEvent?) = validate()
    })
    val result = if (dialog.showAndGet()) {
      passwordField.password.joinToString("").toRString()
    }
    else {
      getRNull()
    }
    promise.setResult(result)
  }
  return promise
}

fun showQuestion(args: RObject): Promise<RObject> {
  val (title, message, ok, cancel) = args.rString.stringsList
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val result = showOkCancelDialog(title, message, ok, cancel, QuestionDialog)
    promise.setResult((result == Messages.OK).toRBoolean())
  }
  return promise
}

fun showPrompt(args: RObject): Promise<RObject> {
  val (title, message, default) = args.rString.stringsList
  val textField = JBTextField()
  textField.text = default
  val panel = panel {
    noteRow(message)
    row { textField().focused() }
  }
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val result = if (dialog(title, panel).showAndGet()) {
      textField.text.toRString()
    }
    else getRNull()
    promise.setResult(result)
  }
  return promise
}

fun askForSecret(args: RObject): Promise<RObject> {
  val (name, message, title) = args.rString.stringsList
  val secretField = JPasswordField()
  val checkBox = JBCheckBox("Remember with keyring")
  val panel = panel {
    noteRow(message)
    row { secretField().focused() }
    row { checkBox() }
    noteRow("""<a href="https://support.rstudio.com/hc/en-us/articles/360000969634">Using Keyring</a>""")
  }.withPreferredWidth(350)
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val dialog = dialog(title, panel)
    dialog.isOKActionEnabled = false
    val validate: () -> Unit = {
      dialog.isOKActionEnabled = secretField.password.isNotEmpty()
    }
    secretField.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent?) = validate()
      override fun insertUpdate(e: DocumentEvent?) = validate()
      override fun removeUpdate(e: DocumentEvent?) = validate()
    })
    val result = if (dialog.showAndGet()) {
      if (checkBox.isSelected) {
        // TODO
      }
      secretField.password.joinToString("").toRString()
    }
    else getRNull()
    promise.setResult(result)
  }
  return promise
}

fun selectFile(args: RObject): Promise<RObject> {
  TODO()
}

fun selectDirectory(args: RObject): Promise<RObject> {
  TODO()
}

fun showDialog(args: RObject): Promise<RObject> {
  val (title, message, url) = args.rString.stringsList
  val msg = "$message\n<a href=\"$url\">$url</a>"
  val promise = AsyncPromise<RObject>()
  runInEdt {
    Messages.showInfoMessage(msg, title)
    promise.setResult(getRNull())
  }
  return promise
}

fun updateDialog(args: RObject): Promise<RObject> {
  TODO()
}

fun getThemeInfo(): RObject {
  // TODO global, foreground, background
  return RObject.newBuilder()
    .setNamedList(RObject.NamedList.newBuilder()
                    .addRObjects(0, RObject.KeyValue.newBuilder().setKey("editor").setValue(UIManager.getLookAndFeel().name.toRString()))
                    .addRObjects(1, RObject.KeyValue.newBuilder().setKey("global").setValue(getRNull()))
                    .addRObjects(2, RObject.KeyValue.newBuilder().setKey("dark").setValue(UIUtil.isUnderDarcula().toRBoolean()))
                    .addRObjects(3, RObject.KeyValue.newBuilder().setKey("foreground").setValue(getRNull()))
                    .addRObjects(4, RObject.KeyValue.newBuilder().setKey("background").setValue(getRNull()))
    ).build()
}

fun jobRunScript(rInterop: RInterop, args: RObject): Promise<RObject> {
  val path = args.list.getRObjects(0).rString.getStrings(0)
  val file = rInterop.interpreter.findFileByPathAtHost(path)
  val workingDir = if (args.list.getRObjects(3).hasRnull()) {
    PathUtil.getParentPath(path)
  }
  else {
    args.list.getRObjects(3).rString.getStrings(0)
  }
  val importEnv = args.list.getRObjects(4).rboolean.getBooleans(0)
  val exportEnv = when (args.list.getRObjects(5).rString.getStrings(0)) {
    "" -> ExportGlobalEnvPolicy.DO_NO_EXPORT
    "R_GlobalEnv" -> ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV
    else -> ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE
  }
  val promise = AsyncPromise<RObject>()
  if (file != null && workingDir != null) {
    RJobRunner.getInstance(rInterop.project).runRJob(RJobTask(file, workingDir, importEnv, exportEnv)).then {
      promise.setResult("${it.startedAt.time} ${it.scriptFile.name}".toRString())
    }
  }
  else {
    promise.setResult(getRNull())
  }
  return promise
}

fun jobRemove(rInterop: RInterop, args: RObject): RObject {
  val id = args.rString.getStrings(0).split(" ", limit = 2)
  val time = id[0].toLong()
  val name = id[1]
  val jobList = RConsoleToolWindowFactory.getJobsPanel(rInterop.project)?.jobList ?: return getRNull()
  jobList.removeJobEntity(jobList.jobEntities.find {
    it.jobDescriptor.startedAt.time == time && it.jobDescriptor.scriptFile.name == name
  } ?: return getRNull())
  return getRNull()
}

fun restartSession(rInterop: RInterop): Promise<RInterpreter> {
  // TODO
  getConsoleView(rInterop)?.print("\nRestarting R session...\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
  return RInterpreterManager.restartInterpreter(rInterop.project)
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

private fun String.toRString(): RObject {
  return RObject.newBuilder().setRString(RObject.RString.newBuilder().addStrings(this)).build()
}

private fun <T : Iterable<RObject>> T.toRList(): RObject {
  return RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(this)).build()
}

private fun Boolean.toRBoolean(): RObject {
  return RObject.newBuilder().setRboolean(RObject.RBoolean.newBuilder().addBooleans(this)).build()
}

private fun getRNull(): RObject {
  return RObject.newBuilder().setRnull(RObject.RNull.getDefaultInstance()).build()
}

private fun getConsoleView(rInterop: RInterop): RConsoleView? {
  return if (RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.rInterop == rInterop) {
    RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull
  }
  else {
    RConsoleManager.getInstance(rInterop.project).consoles.find { it.rInterop == rInterop }
  }
}