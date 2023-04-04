package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import org.jetbrains.r.RLanguage

private class RmdCellLanguageProviderForR : RmdCellLanguageProvider {
  override fun getLanguages(): Map<String, Language> {
    return hashMapOf("r" to RLanguage.INSTANCE)
  }
}