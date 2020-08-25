package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtilRt
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.compute
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.isLocal
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

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
  DOCUMENT_CLOSE_ID;

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
        else -> throw IllegalArgumentException("Unknown function id")
      }
    }
  }
}

fun viewer(rInterop: RInterop, args: RObject): AsyncPromise<Unit> {
  val url = args.list.getRObjects(0).rString.getStrings(0)
  val promise = AsyncPromise<Unit>()
  invokeLater {
    promise.compute {
      RToolWindowFactory.showUrl(rInterop.project, url)
    }
  }
  return promise
}

fun versionInfoMode(rInterop: RInterop): RObject {
  val mode = if (rInterop.interpreter.isLocal()) "desktop" else "server"
  return mode.toRString()
}

internal fun String.toRString(): RObject {
  return RObject.newBuilder().setRString(RObject.RString.newBuilder().addStrings(this)).build()
}

internal fun <T : Iterable<RObject>> T.toRList(): RObject {
  return RObject.newBuilder().setList(RObject.List.newBuilder().addAllRObjects(this)).build()
}

internal fun Boolean.toRBoolean(): RObject {
  return RObject.newBuilder().setRboolean(RObject.RBoolean.newBuilder().addBooleans(this)).build()
}

internal fun Long.toRInt(): RObject {
  return RObject.newBuilder().setRInt(RObject.RInt.newBuilder().addInts(this)).build()
}

internal fun Int.toRInt(): RObject {
  return RObject.newBuilder().setRInt(RObject.RInt.newBuilder().addInts(this.toLong())).build()
}

internal fun getRNull(): RObject {
  return RObject.newBuilder().setRnull(RObject.RNull.getDefaultInstance()).build()
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
  } else {
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