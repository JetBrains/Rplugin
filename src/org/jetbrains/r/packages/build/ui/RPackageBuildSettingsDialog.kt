/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.build.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.r.RBundle
import org.jetbrains.r.settings.RPackageBuildSettings
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty

class RPackageBuildSettingsDialog(project: Project) : DialogWrapper(null, true) {
  private val settings = RPackageBuildSettings.getInstance(project)
  private val installArgsTextField = JBTextField()
  private val checkArgsTextField = JBTextField()

  private var mainArchitectureOnly = settings.mainArchitectureOnly
  private var useDevTools = settings.useDevTools
  private var keepSources = settings.keepSources
  private var cleanBuild = settings.cleanBuild
  private var asCran = settings.asCran

  private var installArgs: List<String>? by ArgsFieldDelegate(installArgsTextField, INSTALL_ARGS_HINT)
  private var checkArgs: List<String>? by ArgsFieldDelegate(checkArgsTextField, CHECK_ARGS_HINT)

  init {
    installArgs = settings.installArgs
    checkArgs = settings.checkArgs
    isResizable = false
    title = RBundle.message("packages.build.settings.title")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      group(RBundle.message("packages.build.settings.general")) {
        row {
          checkBox(RBundle.message("packages.build.settings.use.dev.tools"))
            .bindSelected(::useDevTools)
        }
      }
      group(RBundle.message("packages.build.settings.install.options")) {
        row {
          checkBox(RBundle.message("packages.build.settings.main.architecture.only"))
            .bindSelected(::mainArchitectureOnly)
        }
        row {
          checkBox(RBundle.message("packages.build.settings.keep.sources"))
            .bindSelected(::keepSources)
        }
        row {
          checkBox(RBundle.message("packages.build.settings.clean.build"))
            .bindSelected(::cleanBuild)
        }
        row {
          cell(installArgsTextField)
            .align(AlignX.FILL)
        }
      }
      group(RBundle.message("packages.build.settings.check.options")) {
        row {
          checkBox(RBundle.message("packages.build.settings.as.cran"))
            .bindSelected(::asCran)
        }
        row {
          cell(checkArgsTextField)
            .align(AlignX.FILL)
        }
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    storeCurrentSettings()
  }

  private fun storeCurrentSettings() {
    settings.mainArchitectureOnly = mainArchitectureOnly
    settings.useDevTools = useDevTools
    settings.keepSources = keepSources
    settings.cleanBuild = cleanBuild
    settings.asCran = asCran
    settings.installArgs = installArgs ?: emptyList()
    settings.checkArgs = checkArgs ?: emptyList()
  }

  private fun updateOkAction() {
    isOKActionEnabled = installArgs != null && checkArgs != null
  }

  private inner class ArgsFieldDelegate(private val field: JBTextField, hint: String) {
    init {
      field.emptyText.text = hint
      setupInputListener()
    }

    private fun setupInputListener() {
      field.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          val (_, errorText) = tryParseArgs()
          setErrorText(errorText, field)
          updateOkAction()
        }
      })
    }

    private fun tryParseArgs(): Pair<List<String>?, String?> {
      val text = field.text
      if (text.isNullOrBlank()) {
        return Pair(emptyList(), null)
      }
      val tokens = text.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
      val args = mutableListOf<String>()
      for (token in tokens) {
        when {
          token.startsWith("--") -> {
            if (token.length > 2) {
              args.add(token)
            }
          }
          token.startsWith('-') -> {
            if (token.length > 2) {
              return Pair(null, createTooManyLettersForShortArgumentMessage(token))
            }
            if (token.length == 2) {
              args.add(token)
            }
          }
          else -> {
            return Pair(null, createIncorrectArgumentPrefixMessage(token))
          }
        }
      }
      return Pair(args, null)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): List<String>? {
      return tryParseArgs().first
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: List<String>?) {
      field.text = value?.joinToString(separator = " ") ?: ""
    }
  }

  companion object {
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val INSTALL_ARGS_HINT = RBundle.message("packages.build.settings.install.args.hint")
    private val CHECK_ARGS_HINT = RBundle.message("packages.build.settings.check.args.hint")

    private fun createTooManyLettersForShortArgumentMessage(argument: String): String {
      return RBundle.message("packages.build.settings.incorrect.short.argument", argument)
    }

    private fun createIncorrectArgumentPrefixMessage(argument: String): String {
      return RBundle.message("packages.build.settings.incorrect.argument.prefix", argument)
    }
  }
}
