/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.util.PathUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.*

class RAddInterpreterDialog {
  companion object {
    private val homeChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
      .withFileFilter { it.name.let { name -> name == "R" || name == "R.exe" } }
      .withTitle(RBundle.message("project.settings.select.interpreter"))
      .withShowHiddenFiles(SystemInfo.isUnix)

    fun show(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
      FileChooser.chooseFiles(homeChooserDescriptor, null, null) { files ->
        files.firstOrNull()?.let {
          fun tryGetInterpreterVersion(path: String): Pair<Version?, Exception?> {
            return try {
              val version = ExecuteExpressionUtils.getSynchronously(RBundle.message("project.settings.check.interpreter")) {
                RInterpreterUtil.getVersionByPath(path)
              }
              Pair(version, null)
            } catch (e: Exception) {
              Pair(null, e)
            }
          }

          fun checkDuplicate(interpreters: List<RInterpreterInfo>, location: RInterpreterLocation): Boolean {
            return !interpreters.any { i -> i.interpreterLocation == location }
          }

          val path = PathUtil.toSystemDependentName(it.path)
          val location = RLocalInterpreterLocation(path)
          if (checkDuplicate(existingInterpreters, location)) {
            val (version, e) = tryGetInterpreterVersion(path)
            if (version != null && (RInterpreterUtil.isSupportedVersion(version) || showYesNoDialog(
                RBundle.message("project.settings.not.supported.interpreter.title"),
                RBundle.message("project.settings.not.supported.interpreter.warning", version.toString()), null))) {
              val interpreter = RBasicInterpreterInfo(RBundle.message("project.settings.added.interpreter"), location, version)
              onAdded(interpreter)
            } else {
              @Suppress("HardCodedStringLiteral")
              val details = e?.message?.let { m -> ":\n$m" } ?: ""
              Messages.showErrorDialog("${RBundle.message("project.settings.invalid.interpreter.description")}$details", RBundle.message("project.settings.invalid.interpreter"))
            }
          } else {
            Messages.showErrorDialog(RBundle.message("project.settings.interpreter.duplicate.description"), RBundle.message("project.settings.interpreter.duplicate.title"))
          }
        }
      }
    }
  }
}
