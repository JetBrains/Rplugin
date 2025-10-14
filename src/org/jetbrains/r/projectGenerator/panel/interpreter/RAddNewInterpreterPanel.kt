/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.interpreter

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginUtil
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.r.psi.interpreter.RInterpreterLocation
import org.jetbrains.annotations.Nls
import org.jetbrains.r.configuration.RManageInterpreterPanel
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.getVersion
import java.awt.BorderLayout

class RAddNewInterpreterPanel(existingInterpreters: List<RInterpreterInfo>, localOnly: Boolean) : RInterpreterPanel() {
  override val panelName = PANEL_NAME

  private val manageInterpreterPanel = RManageInterpreterPanel(PANEL_HINT, localOnly) {
    runListeners()
  }
  private val lastValidatedInterpreter = LastValidatedInterpreter()

  override val interpreterLocation: RInterpreterLocation?
    get() = manageInterpreterPanel.currentSelection?.interpreterLocation

  init {
    layout = BorderLayout()
    manageInterpreterPanel.initialInterpreters = existingInterpreters
    manageInterpreterPanel.initialSelection = existingInterpreters.firstOrNull()
    manageInterpreterPanel.reset()
    add(manageInterpreterPanel.component, BorderLayout.NORTH)
  }

  override fun fetchInstalledPackages(): List<String> {
    val location = interpreterLocation ?: return emptyList()
    return ExecuteExpressionUtils.getSynchronously(FETCH_INSTALLED_PACKAGES) {
      location.uploadFileToHost(FETCH_PACKAGES_PATH)
      RInterpreterUtil.runHelper(location, FETCH_PACKAGES_PATH, null, emptyList()).lines().drop(1)
    }
  }

  override fun validateInterpreter(): List<ValidationInfo> {
    val location = interpreterLocation ?: return listOf(ValidationInfo(MISSING_INTERPRETER_TEXT))
    if (location == lastValidatedInterpreter.location) return lastValidatedInterpreter.validationInfo
    lastValidatedInterpreter.location = location
    lastValidatedInterpreter.validationInfo = if (!isCorrectInterpreterLocation(location)) {
      listOf(ValidationInfo(INVALID_INTERPRETER_TEXT))
    } else {
      emptyList()
    }
    return lastValidatedInterpreter.validationInfo
  }

  private fun isCorrectInterpreterLocation(interpreterLocation: RInterpreterLocation): Boolean {
    return try {
      val version = ExecuteExpressionUtils.getSynchronously(CHECK_INTERPRETER_TITLE) {
        interpreterLocation.getVersion()
      }
      RInterpreterUtil.isSupportedVersion(version)
    } catch (_: Exception) {
      false
    }
  }

  private data class LastValidatedInterpreter(var location: RInterpreterLocation? = null,
                                              var validationInfo: List<ValidationInfo> = emptyList())

  companion object {
    private val PANEL_NAME = RBundle.message("project.settings.new.interpreter")
    @Nls
    private val PANEL_HINT = RBundle.message("project.settings.base.interpreter")
    private val CHECK_INTERPRETER_TITLE = RBundle.message("project.settings.check.interpreter")
    private val MISSING_INTERPRETER_TEXT = RBundle.message("project.settings.missing.interpreter")
    private val INVALID_INTERPRETER_TEXT = RBundle.message("project.settings.invalid.interpreter")
    private val FETCH_INSTALLED_PACKAGES = RBundle.message("project.settings.fetch.installed.packages")

    private val FETCH_PACKAGES_PATH by lazy { RPluginUtil.findFileInRHelpers("R/projectGenerator/getAllInstalledPackages.R") }
  }
}