package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.r.psi.RLanguage

private class RmdCellLanguageProviderForR : RmdCellLanguageProvider {
  override fun getLanguages(): Map<String, Language> {
    return hashMapOf("r" to RLanguage.INSTANCE)
  }
}