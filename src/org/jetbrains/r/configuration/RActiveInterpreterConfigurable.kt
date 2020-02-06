/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class RActiveInterpreterConfigurable(private val project: Project) : UnnamedConfigurable {
  private val settings = RSettings.getInstance(project)
  private val panel = RManageInterpreterPanel(RBundle.message("project.settings.interpreter.label"), false, null)
  private val wrapper = JPanel(BorderLayout())

  init {
    reset()
    wrapper.add(panel.component, BorderLayout.NORTH)
  }

  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun reset() {
    fun String.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return if (this.isNotBlank()) existing.find { it.interpreterPath == this } else null
    }

    val existing = getSynchronously(LOADING_INTERPRETERS_TEXT) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val selection = settings.interpreterPath.findAmong(existing)
    panel.initialSelection = selection
    panel.initialInterpreters = existing
    panel.reset()
  }

  override fun apply() {
    RInterpreterSettings.setEnabledInterpreters(panel.currentInterpreters)
    val path = panel.currentSelection?.interpreterPath ?: ""
    val previousPath = settings.interpreterPath
    if (path != previousPath) {
      settings.interpreterPath = path
      RConsoleManager.closeMismatchingConsoles(project, path)
      restartInterpreter()
      RConsoleManager.runConsole(project)
    }
    reset()
  }

  override fun createComponent(): JComponent {
    return wrapper
  }

  private fun restartInterpreter() {
    RInterpreterManager.getInstance(project).initializeInterpreter(true).onSuccess {
      RepoProvider.getInstance(project).onInterpreterVersionChange()
      ApplicationManager.getApplication().invokeLater {
        getPackagesPanel(project).scheduleRefresh()
      }
    }
  }

  companion object {
    private val LOADING_INTERPRETERS_TEXT = RBundle.message("project.settings.interpreters.loading")

    private fun getPackagesPanel(project: Project) =
      RToolWindowFactory.findContent(project, RToolWindowFactory.PACKAGES).component as RInstalledPackagesPanel
  }
}
