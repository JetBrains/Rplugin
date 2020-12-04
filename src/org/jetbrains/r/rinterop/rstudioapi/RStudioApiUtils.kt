package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtilRt
import git4idea.changes.GitChangesViewRefresher
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.isLocal
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import kotlin.math.min
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
  DOCUMENT_NEW_ID,
  TERMINAL_ACTIVATE_ID,
  TERMINAL_BUFFER_ID,
  TERMINAL_BUSY_ID,
  TERMINAL_CLEAR_ID,
  TERMINAL_CONTEXT_ID,
  TERMINAL_CREATE_ID,
  TERMINAL_EXECUTE_ID,
  TERMINAL_EXITCODE_ID,
  TERMINAL_KILL_ID,
  TERMINAL_LIST_ID,
  TERMINAL_RUNNING_ID,
  TERMINAL_SEND_ID,
  TERMINAL_VISIBLE_ID,
  VIEWER_ID,
  VERSION_INFO_MODE_ID,
  DOCUMENT_CLOSE_ID,
  SOURCE_MARKERS_ID,
  TRANSLATE_LOCAL_URL_ID,
  EXECUTE_COMMAND_ID,
  OPEN_PROJECT_ID,
  WRITE_PROJECT_FILE;

  companion object {
    fun fromInt(a: Int): RStudioApiFunctionId? {
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
        22 -> TERMINAL_ACTIVATE_ID
        23 -> TERMINAL_BUFFER_ID
        24 -> TERMINAL_BUSY_ID
        25 -> TERMINAL_CLEAR_ID
        26 -> TERMINAL_CONTEXT_ID
        27 -> TERMINAL_CREATE_ID
        28 -> TERMINAL_EXECUTE_ID
        29 -> TERMINAL_EXITCODE_ID
        30 -> TERMINAL_KILL_ID
        31 -> TERMINAL_LIST_ID
        32 -> TERMINAL_RUNNING_ID
        33 -> TERMINAL_SEND_ID
        34 -> TERMINAL_VISIBLE_ID
        35 -> VIEWER_ID
        36 -> VERSION_INFO_MODE_ID
        37 -> DOCUMENT_CLOSE_ID
        38 -> SOURCE_MARKERS_ID
        39 -> TRANSLATE_LOCAL_URL_ID
        40 -> EXECUTE_COMMAND_ID
        41 -> OPEN_PROJECT_ID
        42 -> WRITE_PROJECT_FILE
        else -> null
      }
    }
  }
}

object RStudioApiUtils {
  fun viewer(rInterop: RInterop, args: RObject) {
    val url = args.list.getRObjects(0).rString.getStrings(0)
    rInterop.interpreter.showUrlInViewer(rInterop, url)
  }

  fun sourceMarkers(rInterop: RInterop, args: RObject) {
    val name = args.list.getRObjects(0).rString.getStrings(0)
    val markers = args.list.getRObjects(1).list.rObjectsList.map { marker ->
      marker.list.rObjectsList
    }
    val basePath = args.list.getRObjects(2).toStringOrNull()
    var autoSelect = args.list.getRObjects(3).rString.getStrings(0).let {
      if (it == "none") null
      else it
    }
    val files = markers.map { it[1].rString.getStrings(0) }.toHashSet()
    for (file in files) {
      val filePath = (basePath ?: "") + file
      findFileByPathAtHostHelper(rInterop, filePath).then { virtualFile ->
        val document = virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) }
        if (document == null) {
          return@then
        }
        val editors = EditorFactory.getInstance().editors(document, rInterop.project).toList()
        for (e in editors) {
          e.markupModel.allHighlighters.filter { it.textAttributesKey?.externalName == name }.map {
            e.markupModel.removeHighlighter(it)
          }
        }
      }
    }
    for (marker in markers) {
      val type = marker[0].rString.getStrings(0)
      val file = marker[1].rString.getStrings(0)
      val line = marker[2].rInt.getInts(0)
      val column = marker[3].rInt.getInts(0)
      val message = marker[4].rString.getStrings(0)
      val filePath = (basePath ?: "") + file
      val highlighterLayer = when (type) {
        "error" -> HighlighterLayer.ERROR
        "warning", "style" -> HighlighterLayer.WARNING
        "info", "usage" -> HighlighterLayer.WEAK_WARNING
        else -> return
      }
      findFileByPathAtHostHelper(rInterop, filePath).then { virtualFile ->
        val document = virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) }
        if (document == null) {
          return@then
        }
        val editors = EditorFactory.getInstance().editors(document, rInterop.project).toList()
        for (e in editors) {
          val offset = getLineOffset(document, line - 1, column - 1)
          if (autoSelect == "first" || (autoSelect == "error" && type == "error")) {
            e.caretModel.primaryCaret.moveToOffset(offset)
          }
          val textAttributesKey = TextAttributesKey.createTextAttributesKey(name)
          val color = e.colorsScheme.getAttributes(
            when (highlighterLayer) {
              HighlighterLayer.ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
              HighlighterLayer.WARNING -> CodeInsightColors.WARNINGS_ATTRIBUTES
              HighlighterLayer.WEAK_WARNING -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
              else -> return@then
            }
          ).errorStripeColor
          e.markupModel.addLineHighlighter(line.toInt() - 1, highlighterLayer, null)
          val rangeHighlighter = e.markupModel.addRangeHighlighter(
            textAttributesKey,
            offset, offset,
            highlighterLayer,
            HighlighterTargetArea.LINES_IN_RANGE
          )
          rangeHighlighter.errorStripeMarkColor = color
          rangeHighlighter.errorStripeTooltip = message
        }
      }
      if (autoSelect == "first" || type == "error") {
        autoSelect = null
      }
    }
  }

  fun versionInfoMode(rInterop: RInterop): RObject {
    val mode = if (rInterop.interpreter.isLocal()) "desktop" else "server"
    return mode.toRString()
  }

  fun translateLocalUrl(rInterop: RInterop, args: RObject): Promise<RObject> {
    val url = args.list.getRObjects(0).rString.getStrings(0)
    val absolute = args.list.getRObjects(1).rBoolean.getBooleans(0)
    val promise = AsyncPromise<RObject>()
    rInterop.interpreter.translateLocalUrl(rInterop, url, absolute).then {
      promise.setResult(it.toRString())
    }
    return promise
  }

  fun executeCommand(rInterop: RInterop, args: RObject) {
    when (val command = args.list.getRObjects(0).rString.getStrings(0)) {
      "vcsRefresh" -> {
        invokeLater {
          GitChangesViewRefresher().refresh(rInterop.project)
        }
      }
      else -> {
        val quiet = args.list.getRObjects(1).rBoolean.getBooleans(0)
        if (!quiet) {
          runInEdt {
            Messages.showErrorDialog(
              rInterop.project,
              "The command '$command' is unsupported or does not exist.",
              "Invalid Command"
            )
          }
        }
      }
    }
  }

  private fun getLineOffset(document: Document, line: Long, col: Long): Int {
    return min(document.getLineEndOffset(line.toInt()), document.getLineStartOffset(line.toInt()) + col.toInt())
  }
}

internal fun String.toRString(): RObject {
  return RObject.newBuilder().setRString(RObject.RString.newBuilder().addStrings(this)).build()
}

internal fun RObject.toStringOrNull(): String? {
  return if (this.hasRNull()) return null
  else {
    this.rString.getStrings(0)
  }
}

internal fun <T : Iterable<RObject>> T.toRList(): RObject {
  return RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(this)).build()
}

internal fun Boolean.toRBoolean(): RObject {
  return RObject.newBuilder().setRBoolean(RObject.RBoolean.newBuilder().addBooleans(this)).build()
}

internal fun Long.toRInt(): RObject {
  return RObject.newBuilder().setRInt(RObject.RInt.newBuilder().addInts(this)).build()
}

internal fun Int.toRInt(): RObject {
  return RObject.newBuilder().setRInt(RObject.RInt.newBuilder().addInts(this.toLong())).build()
}

internal fun getRNull(): RObject {
  return RObject.newBuilder().setRNull(RObject.RNull.getDefaultInstance()).build()
}

internal fun getConsoleView(rInterop: RInterop): RConsoleView? {
  return if (RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull?.rInterop == rInterop) {
    RConsoleManager.getInstance(rInterop.project).currentConsoleOrNull
  }
  else {
    RConsoleManager.getInstance(rInterop.project).consoles.find { it.rInterop == rInterop }
  }
}

internal fun rError(message: String): RObject {
  return RObject.newBuilder().setError(message).build()
}

internal fun findFileByPathAtHostHelper(rInterop: RInterop, path: String): Promise<VirtualFile?> {
  val promise = AsyncPromise<VirtualFile?>()
  if (rInterop.interpreter.isLocal()) {
    promise.setResult(rInterop.interpreter.findFileByPathAtHost(path))
  }
  else {
    val name = PathUtilRt.getFileName(path)
    ProgressManager.getInstance().run(object : Task.Backgroundable(
      rInterop.project, "remote.host.view.opening.file.title.$name") {
      override fun run(indicator: ProgressIndicator) {
        val file = rInterop.interpreter.findFileByPathAtHost(path)
        promise.setResult(file)
      }
    })
  }
  return promise
}