package org.jetbrains.r.rStudioProjects

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import org.jetbrains.r.RLanguage
import java.nio.charset.Charset

data class RStudioProjectSettings(
  val numSpacesForTab: Int? = null,
  val useTabCharacter: Boolean? = null,
  val rVersion: Version? = null,
  val saveWorkspace: YesNoAskValue = YesNoAskValue.DEFAULT,
  val loadWorkspace: YesNoAskValue = YesNoAskValue.DEFAULT,
  val alwaysSaveHistory: YesNoAskValue = YesNoAskValue.DEFAULT,
  val lineSeparator: String? = null,
  val encoding: Charset? = null,
  val rnwWeave: SweaveEngine = SweaveEngine.SWEAVE,
  val latex: LatexProgram = LatexProgram.PDFLATEX
) {
  companion object {
    val DEFAULT_INSTANCE = RStudioProjectSettings(
      numSpacesForTab = CodeStyle.getDefaultSettings().getLanguageIndentOptions(RLanguage.INSTANCE).TAB_SIZE,
      useTabCharacter = CodeStyle.getDefaultSettings().getLanguageIndentOptions(RLanguage.INSTANCE).USE_TAB_CHARACTER,
      encoding = EncodingProjectManager.getInstance().defaultConsoleEncoding
    )
  }
}

enum class YesNoAskValue {
  YES,
  NO,
  ASK,
  DEFAULT;

  override fun toString(): String {
    return when (this) {
      YES -> "Yes"
      NO -> "No"
      ASK -> "Ask"
      DEFAULT -> "Default"
    }
  }
}

enum class SweaveEngine {
  KNITR,
  SWEAVE;

  override fun toString(): String {
    return when (this) {
      KNITR -> "knitr"
      SWEAVE -> "Sweave"
    }
  }
}

enum class LatexProgram {
  XELATEX,
  PDFLATEX;

  override fun toString(): String {
    return when (this) {
      XELATEX -> "XeLaTeX"
      PDFLATEX -> "pdfLaTeX"
    }
  }
}