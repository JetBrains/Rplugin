/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle
import javax.swing.JComponent

class RActiveInterpreterProjectConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
  private var configurable: RActiveInterpreterConfigurable? = null

  private val guaranteedConfigurable: RActiveInterpreterConfigurable
    get() = configurable ?: RActiveInterpreterConfigurable(project).also {
      configurable = it
    }

  override fun isModified(): Boolean {
    return configurable?.isModified ?: false
  }

  override fun getHelpTopic(): String? {
    return HELP_TOPIC
  }

  override fun getId(): String {
    return RActiveInterpreterProjectConfigurable::class.qualifiedName ?: ""
  }

  override fun getDisplayName(): String {
    return NAME
  }

  override fun apply() {
    configurable?.apply()
  }

  override fun createComponent(): JComponent {
    return guaranteedConfigurable.createComponent()
  }

  override fun reset() {
    configurable?.reset()
  }

  override fun disposeUIResources() {
    configurable?.disposeUIResources()
    configurable = null
  }

  companion object {
    private const val HELP_TOPIC = "reference.settings.project.interpreter"
    private val NAME = RBundle.message("project.settings.module.name")
  }
}
