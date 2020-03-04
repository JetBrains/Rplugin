/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.build.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.build.ui.forms.RPackageBuildSettingsForm
import org.jetbrains.r.settings.RPackageBuildSettings
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty

class RPackageBuildSettingsDialog(project: Project) : DialogWrapper(null, true) {
  private val form = RPackageBuildSettingsForm()
  private val settings = RPackageBuildSettings.getInstance(project)

  private var mainArchitectureOnly by CheckboxDelegate(form.mainArchitectureCheckBox)
  private var useDevTools by CheckboxDelegate(form.useDevToolsCheckBox)
  private var keepSources by CheckboxDelegate(form.keepSourcesCheckBox)
  private var cleanBuild by CheckboxDelegate(form.cleanBuildCheckBox)
  private var asCran by CheckboxDelegate(form.asCranCheckBox)

  private var installArgs: List<String>? by ArgsFieldDelegate(form.installArgsTextField, INSTALL_ARGS_HINT)
  private var checkArgs: List<String>? by ArgsFieldDelegate(form.checkArgsTextField, CHECK_ARGS_HINT)

  init {
    loadInitialSettings()
    setResizable(false)
    title = TITLE
    init()
  }

  override fun createCenterPanel(): JComponent {
    return form.contentPane
  }

  override fun doOKAction() {
    super.doOKAction()
    storeCurrentSettings()
  }

  private fun loadInitialSettings() {
    mainArchitectureOnly = settings.mainArchitectureOnly
    useDevTools = settings.useDevTools
    keepSources = settings.keepSources
    cleanBuild = settings.cleanBuild
    asCran = settings.asCran
    installArgs = settings.installArgs
    checkArgs = settings.checkArgs
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

  private class CheckboxDelegate(private val checkbox: JCheckBox) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
      return checkbox.isSelected
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
      checkbox.isSelected = value
    }
  }

  private inner class ArgsFieldDelegate(field: JTextField, hint: String) {
    private val proxy = JBTextField()

    init {
      replaceTextField(field, hint)
      setupInputListener()
    }

    /*
     * The definitive guide to the black magic of GUI hacking below.
     * Given that:
     *   1) a vanilla JTextField doesn't support any grayed out hints for empty input
     *   2) JBTextField supports them but UI Designer doesn't allow to use it instead of JTextField
     * how can one setup a hint for a text field?
     *   The obvious solution is to replace JTextField instantiated by the UI Designer
     * with an instance of JBTextField.
     * To make this possible, an extra (otherwise useless) JPanel with BorderLayout is introduced.
     *   Please note, BorderLayout is essential: it ensures `panel.add(proxy)` statement
     * will work smoothly without any particular layout constraints specified as the second argument
     * whilst GridLayoutManager will throw an error
     */
    private fun replaceTextField(field: JTextField, hint: String) {
      proxy.emptyText.text = hint
      val panel = field.parent
      panel.remove(field)
      panel.add(proxy)
    }

    private fun setupInputListener() {
      proxy.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          val (_, errorText) = tryParseArgs()
          setErrorText(errorText, proxy)
          updateOkAction()
        }
      })
    }

    private fun tryParseArgs(): Pair<List<String>?, String?> {
      val text = proxy.text
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
      proxy.text = value?.joinToString(separator = " ") ?: ""
    }
  }

  companion object {
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val INSTALL_ARGS_HINT = RBundle.message("packages.build.settings.install.args.hint")
    private val CHECK_ARGS_HINT = RBundle.message("packages.build.settings.check.args.hint")
    private val TITLE = RBundle.message("packages.build.settings.title")

    private fun createTooManyLettersForShortArgumentMessage(argument: String): String {
      return RBundle.message("packages.build.settings.incorrect.short.argument", argument)
    }

    private fun createIncorrectArgumentPrefixMessage(argument: String): String {
      return RBundle.message("packages.build.settings.incorrect.argument.prefix", argument)
    }
  }
}
