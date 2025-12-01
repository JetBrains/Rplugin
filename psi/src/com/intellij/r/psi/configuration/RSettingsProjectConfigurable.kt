/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.r.psi.RBundle
import javax.swing.JComponent

class RSettingsProjectConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
  private var configurable: RSettingsConfigurable? = null

  private val guaranteedConfigurable: RSettingsConfigurable
    get() = configurable ?: RSettingsConfigurable(project).also {
      configurable = it
    }

  override fun isModified(): Boolean {
    return configurable?.isModified ?: false
  }

  override fun getHelpTopic(): String {
    return HELP_TOPIC
  }

  override fun getId(): String {
    return RSettingsProjectConfigurable::class.qualifiedName ?: ""
  }

  override fun getDisplayName(): String {
    return RBundle.message("project.settings.module.name")
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
  }
}
