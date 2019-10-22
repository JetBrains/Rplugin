/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.configuration

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil

class RAddInterpreterDialog {
  companion object {
    private val CHECK_INTERPRETER_TITLE = RBundle.message("project.settings.check.interpreter")
    private val INVALID_INTERPRETER_TITLE = RBundle.message("project.settings.invalid.interpreter")
    private val INVALID_INTERPRETER_DESCRIPTION = RBundle.message("project.settings.invalid.interpreter.description")
    private val INTERPRETER_DUPLICATE_TITLE = RBundle.message("project.settings.interpreter.duplicate.title")
    private val INTERPRETER_DUPLICATE_DESCRIPTION = RBundle.message("project.settings.interpreter.duplicate.description")
    private val ADDED_INTERPRETER_NAME = RBundle.message("project.settings.added.interpreter")

    private val homeChooserDescriptor = object : FileChooserDescriptor(true, false, false, false, false, false) {
      override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean {
        fun checkExecutable(file: VirtualFile) : Boolean {
          val name = file.name
          return name == "R" || name == "R.exe"
        }

        return (file.isDirectory || checkExecutable(file)) && super.isFileVisible(file, showHiddenFiles)
      }
    }.withTitle(RBundle.message("project.settings.select.interpreter")).withShowHiddenFiles(SystemInfo.isUnix)

    fun show(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
      FileChooser.chooseFiles(homeChooserDescriptor, null, null) { files ->
        files.firstOrNull()?.let {
          fun tryGetInterpreterVersion(path: String): Pair<Version?, Exception?> {
            return try {
              val version = ExecuteExpressionUtils.getSynchronously(CHECK_INTERPRETER_TITLE) {
                RInterpreterUtil.getVersionByPath(path)
              }
              Pair(version, null)
            } catch (e: Exception) {
              Pair(null, e)
            }
          }

          fun checkDuplicate(interpreters: List<RInterpreterInfo>, path: String): Boolean {
            return interpreters.find { i -> i.interpreterPath == path } == null
          }

          val path = PathUtil.toSystemDependentName(it.path)
          if (checkDuplicate(existingInterpreters, path)) {
            val (version, e) = tryGetInterpreterVersion(path)
            if (version != null && RInterpreterUtil.isSupportedVersion(version)) {
              val interpreter = RBasicInterpreterInfo(ADDED_INTERPRETER_NAME, path, version)
              onAdded(interpreter)
            } else {
              val details = e?.message?.let { m -> ":\n$m" } ?: ""
              Messages.showErrorDialog("$INVALID_INTERPRETER_DESCRIPTION$details", INVALID_INTERPRETER_TITLE)
            }
          } else {
            Messages.showErrorDialog(INTERPRETER_DUPLICATE_DESCRIPTION, INTERPRETER_DUPLICATE_TITLE)
          }
        }
      }
    }
  }
}
