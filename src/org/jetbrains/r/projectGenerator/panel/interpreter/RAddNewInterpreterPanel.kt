/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.interpreter

import com.intellij.openapi.ui.ValidationInfo
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.configuration.RManageInterpreterPanel
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil
import java.awt.BorderLayout

class RAddNewInterpreterPanel(existingInterpreters: List<RInterpreterInfo>) : RInterpreterPanel() {
  override val panelName = PANEL_NAME

  private val manageInterpreterPanel = RManageInterpreterPanel(PANEL_HINT, true) {
    runListeners()
  }

  override val interpreterPath: String?
    get() = manageInterpreterPanel.currentSelection?.interpreterPath

  init {
    layout = BorderLayout()
    manageInterpreterPanel.initialInterpreters = existingInterpreters
    manageInterpreterPanel.initialSelection = existingInterpreters.firstOrNull()
    manageInterpreterPanel.reset()
    add(manageInterpreterPanel.component, BorderLayout.NORTH)
  }

  override fun validateInterpreter(): List<ValidationInfo> {
    val path = interpreterPath ?: return listOf(ValidationInfo(MISSING_INTERPRETER_TEXT))
    if (path == LastValidatedInterpreter.path) return LastValidatedInterpreter.validationInfo
    LastValidatedInterpreter.path = path
    LastValidatedInterpreter.validationInfo = if (!isCorrectInterpreterPath(path)) {
      listOf(ValidationInfo(INVALID_INTERPRETER_TEXT))
    } else {
      emptyList()
    }
    return LastValidatedInterpreter.validationInfo
  }

  private fun isCorrectInterpreterPath(interpreterPath: String): Boolean {
    return try {
      val version = ExecuteExpressionUtils.getSynchronously(CHECK_INTERPRETER_TITLE) {
        RInterpreterUtil.getVersionByPath(interpreterPath)
      }
      RInterpreterUtil.isSupportedVersion(version)
    } catch (_: Exception) {
      false
    }
  }

  private object LastValidatedInterpreter {
    var path: String? = null
    var validationInfo: List<ValidationInfo> = emptyList()
  }

  companion object {
    private val PANEL_NAME = RBundle.message("project.settings.new.interpreter")
    private val PANEL_HINT = RBundle.message("project.settings.base.interpreter")
    private val CHECK_INTERPRETER_TITLE = RBundle.message("project.settings.check.interpreter")
    private val MISSING_INTERPRETER_TEXT = RBundle.message("project.settings.missing.interpreter")
    private val INVALID_INTERPRETER_TEXT = RBundle.message("project.settings.invalid.interpreter")
  }
}