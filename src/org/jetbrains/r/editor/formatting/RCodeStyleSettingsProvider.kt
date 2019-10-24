// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.jetbrains.r.RLanguage

/**
 * This class is based on the Groovy and Json formatter implementation.
 *
 * @author Mikhail Golubev
 * @author Holger Brandl
 */
class RCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
  override fun createConfigurable(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): CodeStyleConfigurable {
    return object : CodeStyleAbstractConfigurable(settings, originalSettings, "R") {
      override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
        val language = RLanguage.INSTANCE
        val currentSettings = currentSettings
        return object : TabbedLanguageCodeStylePanel(language, currentSettings, settings) {
          override fun initTabs(settings: CodeStyleSettings) {
            addIndentOptionsTab(settings)
            addSpacesTab(settings)
            addBlankLinesTab(settings)
            addWrappingAndBracesTab(settings)
          }
        }
      }

      override fun getHelpTopic(): String {
        return "reference.settingsdialog.codestyle.r" //FIXME
      }
    }
  }

  override fun getConfigurableDisplayName(): String {
    return RLanguage.INSTANCE.displayName
  }


  override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings {
    return RCodeStyleSettings(settings)
  }
}
