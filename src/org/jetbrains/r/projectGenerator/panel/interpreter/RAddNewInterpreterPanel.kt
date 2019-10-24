/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.interpreter

import com.intellij.openapi.ui.ValidationInfo
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.configuration.RManageInterpreterPanel
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RBasicInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.R_UNKNOWN
import org.jetbrains.r.settings.RInterpreterSettings
import java.awt.BorderLayout

class RAddNewInterpreterPanel(existingInterpreters: List<String>) : RInterpreterPanel() {
  override val panelName = PANEL_NAME

  private val manageInterpreterPanel = RManageInterpreterPanel(PANEL_HINT, true) {
    runListeners()
  }

  override val interpreterPath: String?
    get() = manageInterpreterPanel.currentSelection?.interpreterPath

  init {
    fun getSuggestedInterpreters(suggestedPaths: List<String>): List<RInterpreterInfo> {
      val existing = RInterpreterSettings.existingInterpreters
      return if (existing.isEmpty()) {
        suggestedPaths.map {
          RBasicInterpreterInfo(SUGGESTED_INTERPRETER_NAME, it, R_UNKNOWN)
        }
      } else {
        existing
      }
    }

    layout = BorderLayout()
    val suggested = getSuggestedInterpreters(existingInterpreters)
    manageInterpreterPanel.initialInterpreters = suggested
    manageInterpreterPanel.initialSelection = suggested.firstOrNull()
    manageInterpreterPanel.reset()
    add(manageInterpreterPanel.component, BorderLayout.NORTH)
  }

  override fun validateInterpreter(): List<ValidationInfo> {
    val path = interpreterPath ?: return listOf(ValidationInfo(MISSING_INTERPRETER_TEXT))
    if (!isCorrectInterpreterPath(path)) {
      return listOf(ValidationInfo(INVALID_INTERPRETER_TEXT))
    }
    return emptyList()
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

  companion object {
    private val PANEL_NAME = RBundle.message("project.settings.new.interpreter")
    private val PANEL_HINT = RBundle.message("project.settings.base.interpreter")
    private val SUGGESTED_INTERPRETER_NAME = RBundle.message("project.settings.suggested.interpreter")
    private val CHECK_INTERPRETER_TITLE = RBundle.message("project.settings.check.interpreter")
    private val MISSING_INTERPRETER_TEXT = RBundle.message("project.settings.missing.interpreter")
    private val INVALID_INTERPRETER_TEXT = RBundle.message("project.settings.invalid.interpreter")
  }
}