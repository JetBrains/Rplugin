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

    fun parseStringToLanguage(cellText: CharSequence, maxLangSizePlusOne: Int): String? {
      val prefix = "```{"

      if (!cellText.startsWith(prefix) || (maxLangSizePlusOne == 1)) return null
      return cellText.drop(prefix.length).take(maxLangSizePlusOne).takeWhile { it.isLetterOrDigit() }.toString()
    }
  }

  fun getLanguages(): Map<String, Language>
}

