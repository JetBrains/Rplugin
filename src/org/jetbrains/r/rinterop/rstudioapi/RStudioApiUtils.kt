package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.analysis.problemsView.toolWindow.ProblemsView
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.changes.ChangeListManagerRefreshHelper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.isLocal
import com.intellij.r.psi.rinterop.RObject
import com.intellij.util.PathUtilRt
import com.intellij.util.containers.ComparatorUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.await
import org.jetbrains.r.console.RConsoleManagerImpl
import org.jetbrains.r.console.RConsoleViewImpl
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.rstudioapi.RStudioAPISourceMarkerInspection.Companion.SOURCE_MARKERS_KEY
import java.util.concurrent.ConcurrentHashMap

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
  EXECUTE_COMMAND_ID;

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
        else -> null
      }
    }
  }
}

object RStudioApiUtils {
  fun viewer(rInterop: RInteropImpl, args: RObject) {
    val url = args.list.getRObjects(0).rString.getStrings(0)
    rInterop.interpreter.showUrlInViewer(rInterop, url)
  }

  fun sourceMarkers(rInterop: RInteropImpl, args: RObject) {
    val name = args.list.getRObjects(0).rString.getStrings(0)
    val markers = args.list.getRObjects(1).list.rObjectsList.map { marker ->
      marker.list.rObjectsList
    }
    val basePath = args.list.getRObjects(2).toStringOrNull()
    var autoSelect = args.list.getRObjects(3).rString.getStrings(0).let {
      if (it == "none") null
      else it
    }

    val projectMarkers = rInterop.project.getUserData(SOURCE_MARKERS_KEY)
                         ?: rInterop.project.putUserData(SOURCE_MARKERS_KEY, ConcurrentHashMap<String, SourceMarkers>()).let {
                           rInterop.project.getUserData(SOURCE_MARKERS_KEY) ?: throw Error("projectMarkers is null")
                         }

    val markerMap = hashMapOf<String, MutableList<RStudioAPISourceMarkerInspection.RStudioAPIMarker>>()

    if (autoSelect == "error" && markers.none { it[0].rString.getStrings(0) == "error" }) {
      autoSelect = "first"
    }

    for (marker in markers) {
      val type = marker[0].rString.getStrings(0)
      val file = marker[1].rString.getStrings(0)
      val line = marker[2].rInt.getInts(0).toInt()
      val column = marker[3].rInt.getInts(0).toInt()
      val message = marker[4].rString.getStrings(0)
      val filePath = (basePath ?: "") + file
      val problemHighlightType = when (type) {
        "error" -> ProblemHighlightType.GENERIC_ERROR
        "warning", "style" -> ProblemHighlightType.WARNING
        "info", "usage" -> ProblemHighlightType.WEAK_WARNING
        else -> return
      }
      if (autoSelect == "first" || (autoSelect == "error" && type == "error")) {
        findFileByPathAtHostHelper(rInterop, filePath).then { virtualFile ->
          FileEditorManager.getInstance(rInterop.project).openFile(virtualFile ?: return@then, true)
          val document = virtualFile.let { FileDocumentManager.getInstance().getDocument(it) }
          if (document == null) {
            return@then
          }
          val editors = EditorFactory.getInstance().editors(document, rInterop.project).toList()
          for (e in editors) {
            val offset = getLineOffset(document, line - 1, column - 1)
            e.caretModel.primaryCaret.moveToOffset(offset)
          }
          ProblemsView.toggleCurrentFileProblems(rInterop.project, virtualFile, document)
        }
        autoSelect = null
      }
      val problem = RStudioAPISourceMarkerInspection.RStudioAPIMarker(problemHighlightType, message, line, column)
      markerMap[filePath]?.add(problem) ?: markerMap.put(filePath, mutableListOf(problem))
    }
    markerMap.map {
      projectMarkers.putIfAbsent(it.key, SourceMarkers())
      projectMarkers[it.key]?.put(name, it.value)
    }
  }

  fun versionInfoMode(rInterop: RInteropImpl): RObject {
    val mode = if (rInterop.interpreter.isLocal()) "desktop" else "server"
    return mode.toRString()
  }

  suspend fun translateLocalUrl(rInterop: RInteropImpl, args: RObject): RObject {
    val url = args.list.getRObjects(0).rString.getStrings(0)
    val absolute = args.list.getRObjects(1).rBoolean.getBooleans(0)
    val result = rInterop.interpreter.translateLocalUrl(rInterop, url, absolute).await()
    return result.toRString()
  }

  fun executeCommand(rInterop: RInteropImpl, args: RObject) {
    when (val command = args.list.getRObjects(0).rString.getStrings(0)) {
      "vcsRefresh" -> {
        invokeLater {
          ChangeListManagerRefreshHelper.refreshSync(rInterop.project)
        }
      }
      else -> {
        val quiet = args.list.getRObjects(1).rBoolean.getBooleans(0)
        if (!quiet) {
          runInEdt {
            Messages.showErrorDialog(
              rInterop.project,
              RBundle.message("rstudioapi.execute.invalid.command.message", command),
              RBundle.message("rstudioapi.execute.invalid.command.title")
            )
          }
        }
      }
    }
  }

  internal fun getLineOffset(document: Document, line: Int, col: Int): Int {
    val lastLine = if (document.lineCount == 0) 0 else document.lineCount - 1
    return when {
      line > document.lineCount -> {
        document.getLineEndOffset(lastLine)
      }
      else -> {
        ComparatorUtil.min(col + document.getLineStartOffset(line), document.getLineEndOffset(line))
      }
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

  internal fun getConsoleView(rInterop: RInteropImpl): RConsoleViewImpl? {
    return if (RConsoleManagerImpl.getInstance(rInterop.project).currentConsoleOrNull?.rInterop == rInterop) {
      RConsoleManagerImpl.getInstance(rInterop.project).currentConsoleOrNull
    }
    else {
      RConsoleManagerImpl.getInstance(rInterop.project).consoles.find { it.rInterop == rInterop }
    }
  }

  internal fun rError(message: String): RObject {
    return RObject.newBuilder().setError(message).build()
  }

  internal fun findFileByPathAtHostHelper(rInterop: RInteropImpl, path: String): Promise<VirtualFile?> {
    val promise = AsyncPromise<VirtualFile?>()
    if (rInterop.interpreter.isLocal()) {
      promise.setResult(rInterop.interpreter.findFileByPathAtHost(path))
    }
    else {
      val name = PathUtilRt.getFileName(path)
      ProgressManager.getInstance().run(object : Task.Backgroundable(
        rInterop.project, RBundle.message("rstudioapi.find.file.at.host", name)) {
        override fun run(indicator: ProgressIndicator) {
          val file = rInterop.interpreter.findFileByPathAtHost(path)
          promise.setResult(file)
        }
      })
    }
    return promise
  }
}
