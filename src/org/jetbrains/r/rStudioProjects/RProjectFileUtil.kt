package org.jetbrains.r.rStudioProjects

import com.intellij.openapi.util.io.FileUtil
import java.io.File

fun writeRProjectFile(settings: RStudioProjectSettings, file: File) {
  val content = """
      Version: 1.0
      ${settings.rVersion?.let { "\nRVersion: ${it}" } ?: ""}
      RestoreWorkspace: ${settings.loadWorkspace}
      SaveWorkspace: ${settings.saveWorkspace}
      AlwaysSaveHistory: ${settings.alwaysSaveHistory}
      
      EnableCodeIndexing: Yes
      UseSpacesForTab: ${boolValueToString(!(settings.useTabCharacter ?: RStudioProjectSettings.DEFAULT_INSTANCE.useTabCharacter!!))}
      NumSpacesForTab: ${settings.numSpacesForTab ?: RStudioProjectSettings.DEFAULT_INSTANCE.numSpacesForTab}
      Encoding: ${settings.encoding ?: RStudioProjectSettings.DEFAULT_INSTANCE.encoding}
      
      RnwWeave: ${settings.rnwWeave}
      LaTeX: ${settings.latex}
      ${settings.lineSeparator?.let { "LineEndingConversion: $it" } ?: ""}
  """.trimIndent()
  FileUtil.createIfNotExists(file)
  FileUtil.writeToFile(file, content)
}

private fun boolValueToString(value: Boolean): String {
  return if (value) "Yes" else "No"
}
