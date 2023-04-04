package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.jetbrains.python.PythonLanguage

private class RmdCellLanguageProviderForPython : RmdCellLanguageProvider {
  override fun getLanguages(): Map<String, Language> {
    return hashMapOf("python" to PythonLanguage.INSTANCE)
  }
}