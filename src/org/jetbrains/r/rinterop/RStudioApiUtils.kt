package org.jetbrains.r.rinterop

import com.intellij.icons.AllIcons.General.QuestionDialog
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.components.*
import com.intellij.ui.layout.*
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
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
  GET_THEME_INFO;

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
      val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull ?: return getRNull()
      val document = currentConsole.consoleEditor.document
      val content = document.text.split(System.lineSeparator())
      id = "c${currentConsole.hashCode()}"
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

  val asStr = { it: Boolean -> if (it) "TRUE" else "FALSE" }

  if (echo) {
    if (rInterop.isInSourceFileExecution.get()) {
      RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.executeText(
        "rstudioapi::sendToConsole(\"$code\", ${asStr(execute)}, ${asStr(echo)}, ${asStr(focus)})")
    }
  }

  val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull

  if (focus && currentConsole != null) {
    val toolWindow = RConsoleToolWindowFactory.getRConsoleToolWindows(currentConsole.project)
    if (toolWindow != null) {
      RConsoleToolWindowFactory.getConsoleContent(currentConsole)?.let { content ->
        toolWindow.activate {
          toolWindow.contentManager.setSelectedContent(content)
        }
      }
    }
  }

  if (execute) {
    currentConsole?.executeText(code)
  }
  else {
    invokeLater {
      val consoleEditor = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.consoleEditor
      consoleEditor?.let {
        runWriteAction {
          it.document.setText(code)
          PsiDocumentManager.getInstance(rInterop.project).commitDocument(it.document)
        }
        it.caretModel.moveToOffset(it.document.textLength)
      }
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

fun askForPassword(rInterop: RInterop, args: RObject) {
  val message = args.list.getRObjects(0).rString.getStrings(0)
  val passwordField = JPasswordField()
  val panel = panel {
    noteRow(message)
    row { passwordField().focused() }
  }
  invokeLater {
    val dialog = dialog("", panel)
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
    else getRNull()
    rInterop.executeAsync(rInterop.asyncStub::rStudioApiResponse, result)
  }
}

fun showQuestion(rInterop: RInterop, args: RObject) {
  val (title, message, ok, cancel) = args.list.getRObjects(0).rString.stringsList
  invokeLater {
    val result = showOkCancelDialog(title, message, ok, cancel, QuestionDialog)
    rInterop.executeAsync(rInterop.asyncStub::rStudioApiResponse, (result == Messages.OK).toRBoolean())
  }
}

fun showPrompt(rInterop: RInterop, args: RObject) {
  val (title, message, default) = args.list.getRObjects(0).rString.stringsList
  val textField = JBTextField()
  textField.text = default
  val panel = panel {
    noteRow(message)
    row { textField().focused() }
  }
  invokeLater {
    val result = if (dialog(title, panel).showAndGet()) {
      textField.text.toRString()
    }
    else getRNull()
    rInterop.executeAsync(rInterop.asyncStub::rStudioApiResponse, result)
  }
}

fun askForSecret(rInterop: RInterop, args: RObject) {
  val (name, message, title) = args.list.getRObjects(0).rString.stringsList
  val secretField = JPasswordField()
  val checkBox = JBCheckBox("Remember with keyring")
  val panel = panel {
    noteRow(message)
    row { secretField().focused() }
    row { checkBox() }
    noteRow("""<a href="https://support.rstudio.com/hc/en-us/articles/360000969634">Using Keyring</a>""")
  }.withPreferredWidth(350)
  invokeLater {
    val dialog = dialog(title, panel)
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
    rInterop.executeAsync(rInterop.asyncStub::rStudioApiResponse, result)
  }
}

fun selectFile(rInterop: RInterop, args: RObject) {
  TODO()
}

fun selectDirectory(rInterop: RInterop, args: RObject) {
  TODO()
}

fun showDialog(rInterop: RInterop, args: RObject) {
  val (title, message, url) = args.list.getRObjects(0).rString.stringsList
  val msg = "$message\n<a href=\"$url\">$url</a>"
  invokeLater {
    Messages.showInfoMessage(msg, title)
    rInterop.executeAsync(rInterop.asyncStub::rStudioApiResponse, getRNull())
  }
}

fun updateDialog(rInterop: RInterop, args: RObject) {
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

private fun getDocumentFromId(id: String?, rInterop: RInterop): Document? {
  if (id == null) {
    return if (rInterop.isInSourceFileExecution.get()) {
      val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor
      val file = editor?.file
      file?.let { FileDocumentManager.getInstance().getDocument(file) }
    }
    else {
      val currentConsole = RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull
      currentConsole?.editorDocument
    }
  }
  val type = id[0]
  val numId = (id.drop(1)).toInt()
  return if (type == 'c') {
    if (RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.hashCode() == numId) {
      RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull!!.consoleEditor.document
    }
    else {
      val console = RConsoleManager.getInstance(rInterop.project).consoles.find { it.hashCode() == numId }
      console?.consoleEditor?.document
    }
  }
  else {
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