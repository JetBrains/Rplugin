package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName

interface RmdCellLanguageProvider {
  companion object {
    val EP_NAME: ExtensionPointName<RmdCellLanguageProvider> = ExtensionPointName.create("com.intellij.rmdCellLanguageProvider")

    fun getAllLanguages(): Map<String, Language> {
      val languageMap: MutableMap<String, Language> = hashMapOf()
      for (extension in EP_NAME.extensionList) {
        languageMap += extension.getLanguages()
      }
      return languageMap
    }
  }

  fun getLanguages(): Map<String, Language>
}

